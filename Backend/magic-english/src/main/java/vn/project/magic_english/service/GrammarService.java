package vn.project.magic_english.service;

import java.util.ArrayList;
import java.util.List;

// import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.project.magic_english.model.Grammar;
import vn.project.magic_english.model.GrammarError;
import vn.project.magic_english.model.User;
import vn.project.magic_english.model.request.CheckGrammarRequest;
import vn.project.magic_english.model.response.GrammarCheckResponse;
import vn.project.magic_english.model.response.ResultPaginationDTO;
import vn.project.magic_english.repository.GrammarRepository;
import vn.project.magic_english.repository.UserRepository;
import vn.project.magic_english.utils.SecurityUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrammarService {

    private final AiClientService aiClientService; // Changed from ChatClient
    private final GrammarRepository grammarRepository;
    private final UserRepository userRepository;
    private final UserAchievementService userAchievementService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache for grammar check results to avoid duplicate AI calls
    private final java.util.concurrent.ConcurrentHashMap<String, GrammarCheckResponse> responseCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Check grammar using AI and save to database
     * Optimized: Return response immediately, save DB async
     */
    public GrammarCheckResponse checkGrammar(CheckGrammarRequest request) {
        // Get current user (lightweight query)
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(
                () -> new RuntimeException("User not authenticated"));
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        String inputText = request.getText();
        log.info("Checking grammar for text length: {}", inputText.length());

        // Check cache first (for duplicate submissions within 5 minutes)
        String cacheKey = user.getId() + "_" + inputText.hashCode();
        GrammarCheckResponse cachedResponse = responseCache.get(cacheKey);
        if (cachedResponse != null) {
            log.info("Cache HIT - Returning cached grammar check result");
            return cachedResponse;
        }

        log.info("Cache MISS - Calling AI for grammar check");

        // Create Grammar entity
        Grammar grammar = new Grammar();
        grammar.setUser(user);
        grammar.setInputText(inputText);

        // Call AI to check grammar (this is the slow part)
        long aiStartTime = System.currentTimeMillis();
        checkGrammarWithAI(grammar);
        log.info("AI check completed in {}ms", System.currentTimeMillis() - aiStartTime);

        // Create response BEFORE saving to DB (faster response)
        GrammarCheckResponse response = GrammarCheckResponse.fromEntity(grammar);

        // Cache immediately so next request is instant
        responseCache.put(cacheKey, response);

        // Auto-clear cache after 5 minutes
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
                .schedule(() -> responseCache.remove(cacheKey), 5, java.util.concurrent.TimeUnit.MINUTES);

        // Save to database ASYNCHRONOUSLY (don't block response)
        saveGrammarAsync(grammar, user);

        log.info("Returning response (DB save happening in background)");
        return response;
    }

    /**
     * Save grammar check to database asynchronously
     * This prevents DB I/O from blocking the API response
     */
    @org.springframework.scheduling.annotation.Async("taskExecutor")
    @Transactional
    public void saveGrammarAsync(Grammar grammar, User user) {
        try {
            Grammar saved = grammarRepository.save(grammar);
            log.info("Grammar check saved to DB with ID: {}", saved.getId());

            // Process achievement check
            Long totalGrammarChecks = grammarRepository.countByUserId(user.getId());
            userAchievementService.checkAndGrantAchievements(user, "grammar_check", totalGrammarChecks);
        } catch (Exception e) {
            log.error("Error saving grammar check async: {}", e.getMessage(), e);
        }
    }

    /**
     * Call AI to check grammar and populate Grammar entity
     */
    private void checkGrammarWithAI(Grammar grammar) {
        String text = grammar.getInputText();
        String prompt = String.format("""
                Analyze the following English text for grammar, spelling, punctuation, and clarity errors.
                Provide a detailed JSON response with the following structure:

                {
                    "score": 85,
                    "correctedText": "The fully corrected version of the text",
                    "errors": [
                        {
                            "errorType": "spelling",
                            "beforeText": "Text before the error",
                            "errorText": "The incorrect text",
                            "correctedText": "The correct text",
                            "afterText": "Text after the error",
                            "explanation": "Giải thích chi tiết về lỗi bằng tiếng Việt",
                            "startPosition": 20,
                            "endPosition": 25
                        }
                    ]
                }

                Text to analyze:
                "%s"

                Rules:
                - errorType must be one of: "spelling", "punctuation", "clarity", "grammar"
                - score is from 0-100 (100 = perfect)
                - beforeText, errorText, afterText should provide context for highlighting
                - explanation MUST be written in Vietnamese language, detailed and educational
                - startPosition and endPosition are character indices in the original text
                - Return ONLY valid JSON, no markdown formatting
                """, text);

        try {
            // Using AiClientService for rotation
            String aiResponse = aiClientService.generate(prompt);

            log.info("AI grammar check response received");
            parseAIResponseIntoEntity(grammar, aiResponse);
        } catch (Exception e) {
            log.error("Error calling AI for grammar check: {}", e.getMessage(), e);
            setFallbackData(grammar);
        }
    }

    private void parseAIResponseIntoEntity(Grammar grammar, String aiResponse) {
        try {
            // Extract JSON from markdown if present
            String json = aiResponse;
            if (json.contains("```json")) {
                json = json.substring(json.indexOf("{"), json.lastIndexOf("}") + 1);
            } else if (json.contains("```")) {
                json = json.substring(json.indexOf("{"), json.lastIndexOf("}") + 1);
            }

            JsonNode node = objectMapper.readTree(json);

            // Set score and corrected text
            grammar.setScore(node.path("score").asInt(100));
            grammar.setCorrectedText(node.path("correctedText").asText(grammar.getInputText()));

            // Parse errors
            JsonNode errorsNode = node.path("errors");
            if (errorsNode.isArray()) {
                for (JsonNode errorNode : errorsNode) {
                    GrammarError error = new GrammarError();
                    error.setErrorType(errorNode.path("errorType").asText("grammar"));
                    error.setBeforeText(errorNode.path("beforeText").asText(""));
                    error.setErrorText(errorNode.path("errorText").asText(""));
                    error.setCorrectedText(errorNode.path("correctedText").asText(""));
                    error.setAfterText(errorNode.path("afterText").asText(""));
                    error.setExplanation(errorNode.path("explanation").asText(""));

                    if (errorNode.has("startPosition")) {
                        error.setStartPosition(errorNode.path("startPosition").asInt());
                    }
                    if (errorNode.has("endPosition")) {
                        error.setEndPosition(errorNode.path("endPosition").asInt());
                    }

                    // Set bidirectional relationship in service
                    error.setGrammar(grammar);
                    grammar.getErrors().add(error);
                }
            }

        } catch (Exception e) {
            log.warn("Failed to parse AI grammar response: {}", e.getMessage());
            setFallbackData(grammar);
        }
    }

    private void setFallbackData(Grammar grammar) {
        grammar.setScore(100);
        grammar.setCorrectedText(grammar.getInputText());
        // No errors - means perfect text
    }

    /**
     * Get all grammar checks for current user with pagination
     */
    public ResultPaginationDTO handleGetAllGrammarChecks(Pageable pageable) {
        // Build Specification for user filter
        Specification<Grammar> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by current user
            String email = SecurityUtil.getCurrentUserLogin().orElseThrow(
                    () -> new RuntimeException("User not authenticated"));
            User user = userRepository.findByEmail(email);
            if (user == null) {
                throw new RuntimeException("User not found");
            }
            predicates.add(criteriaBuilder.equal(root.get("user").get("id"), user.getId()));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        // Execute query with spec
        Page<Grammar> pageGrammar = this.grammarRepository.findAll(spec, pageable);

        // Build response
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageGrammar.getTotalPages());
        mt.setTotal(pageGrammar.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pageGrammar.getContent().stream()
                .map(GrammarCheckResponse::fromEntity)
                .toList());

        return rs;
    }

    /**
     * Get grammar check by ID
     */
    public GrammarCheckResponse getGrammarCheckById(Long id) {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(
                () -> new RuntimeException("User not authenticated"));
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Grammar grammar = grammarRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Grammar check not found"));

        // Check if belongs to current user
        if (grammar.getUser().getId() != user.getId()) {
            throw new RuntimeException("Access denied");
        }

        return GrammarCheckResponse.fromEntity(grammar);
    }

    /**
     * Delete grammar check
     */
    @Transactional
    public void deleteGrammarCheck(Long id) {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(
                () -> new RuntimeException("User not authenticated"));
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Grammar grammar = grammarRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Grammar check not found"));

        // Check if belongs to current user
        if (grammar.getUser().getId() != user.getId()) {
            throw new RuntimeException("Access denied");
        }

        grammarRepository.delete(grammar);
        log.info("Grammar check deleted with ID: {}", id);
    }

}
