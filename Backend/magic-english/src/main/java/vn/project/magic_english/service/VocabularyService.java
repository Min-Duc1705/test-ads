package vn.project.magic_english.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import vn.project.magic_english.model.User;
import vn.project.magic_english.model.Vocabulary;
import vn.project.magic_english.model.request.AddVocabularyRequest;
import vn.project.magic_english.model.response.ResultPaginationDTO;
import vn.project.magic_english.model.response.VocabularyDetailResponse;
import vn.project.magic_english.repository.VocabularyRepository;
import vn.project.magic_english.repository.UserRepository;
import vn.project.magic_english.repository.GrammarRepository;
import vn.project.magic_english.utils.SecurityUtil;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VocabularyService {

    private final AiClientService aiClientService;
    private final VocabularyRepository vocabularyRepository;
    private final UserRepository userRepository;
    private final GrammarRepository grammarRepository;
    private final UserAchievementService userAchievementService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache for preview responses to avoid duplicate AI calls
    private final java.util.concurrent.ConcurrentHashMap<String, VocabularyDetailResponse> previewCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * FR1.1 & FR1.2: Nhập từ mới và tự động làm giàu dữ liệu bằng AI
     */
    @Transactional
    public VocabularyDetailResponse addVocabulary(AddVocabularyRequest request) {
        // Lấy user hiện tại
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(
                () -> new RuntimeException("User not authenticated"));
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        String word = request.getWord();
        log.info("Adding vocabulary: {}", word);

        // Tạo entity và gọi AI để làm giàu dữ liệu trực tiếp
        Vocabulary vocabulary = new Vocabulary();
        vocabulary.setUser(user);
        vocabulary.setWord(word);

        enrichVocabularyWithAI(vocabulary);

        // Lấy audio URL từ Free Dictionary API
        vocabulary.setAudioUrl(fetchAudioUrl(word));

        // Lưu vào database
        Vocabulary saved = vocabularyRepository.save(vocabulary);
        log.info("Vocabulary saved with ID: {}", saved.getId());

        // NOTE: Achievement checking is handled by Flutter calling
        // /user-achievements/check endpoint
        // This allows Flutter to receive the newly unlocked achievements and show popup
        // Old code (removed to avoid duplicate check):
        // Long totalVocabs = vocabularyRepository.countByUserId(user.getId());
        // userAchievementService.checkAndGrantAchievements(user, "vocab_added",
        // totalVocabs);

        return VocabularyDetailResponse.fromEntity(saved);
    }

    /**
     * FR1.2: Gọi AI API để làm giàu dữ liệu trực tiếp vào entity
     */
    private void enrichVocabularyWithAI(Vocabulary vocabulary) {
        String word = vocabulary.getWord();
        String prompt = String.format("""
                Analyze the English word "%s" and provide detailed information in STRICT JSON format:
                {
                    "word": "%s",
                    "ipa": "IPA pronunciation (e.g., /həˈloʊ/)",
                    "meanings": ["nghĩa ngắn gọn 1", "nghĩa ngắn gọn 2", "nghĩa ngắn gọn 3"],
                    "wordType": "loại từ (noun/verb/adjective/adverb/etc)",
                    "examples": [
                        "Example sentence 1 in English",
                        "Example sentence 2 in English",
                        "Example sentence 3 in English"
                    ],
                    "cefrLevel": "CEFR level (A1/A2/B1/B2/C1/C2)"
                }

                CRITICAL RULES:
                - Meanings MUST be a JSON array with comma-separated strings
                - Each meaning in Vietnamese, VERY SHORT (max 2-4 words)
                - NO semicolons inside meanings array
                - Example: ["Lớp học", "Buổi học", "Khóa học"] NOT ["Lớp học; Buổi học"]
                - Only provide core meaning without explanation
                - Provide 2-3 SHORT meanings as separate array elements
                - Provide exactly 3 example sentences in English
                - WordType must be in English (noun, verb, adjective, etc)
                - Return ONLY valid JSON without markdown code blocks
                - DO NOT use semicolons anywhere in the JSON
                """, word, word);

        try {
            // Using AiClientService
            String aiResponse = aiClientService.generate(prompt != null ? prompt : "");

            log.info("AI Response received for: {}", word);
            parseAIResponseIntoEntity(vocabulary, aiResponse);
        } catch (Exception e) {
            log.error("Error calling AI: {}", e.getMessage(), e);
            setFallbackData(vocabulary);
        }
    }

    private void parseAIResponseIntoEntity(Vocabulary vocabulary, String aiResponse) {
        try {
            // Clean up response - remove markdown and extra text
            String json = aiResponse.trim();

            // Remove markdown code blocks
            if (json.contains("```json")) {
                int start = json.indexOf("```json") + 7;
                int end = json.indexOf("```", start);
                json = json.substring(start, end).trim();
            } else if (json.contains("```")) {
                int start = json.indexOf("```") + 3;
                int end = json.indexOf("```", start);
                json = json.substring(start, end).trim();
            }

            // Extract only JSON object
            int jsonStart = json.indexOf("{");
            int jsonEnd = json.lastIndexOf("}");
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                json = json.substring(jsonStart, jsonEnd + 1);
            }

            // Remove any invalid characters that might break JSON
            json = json.replaceAll("[\\x00-\\x1F\\x7F]", ""); // Remove control characters

            log.debug("Cleaned JSON: {}", json);

            JsonNode node = objectMapper.readTree(json);

            vocabulary.setIpa(node.path("ipa").asText(""));
            vocabulary.setWordType(node.path("wordType").asText("unknown"));
            vocabulary.setCefrLevel(node.path("cefrLevel").asText("B1"));

            // Parse meanings with validation
            List<String> meanings = new ArrayList<>();
            JsonNode meaningsNode = node.path("meanings");
            if (meaningsNode.isArray()) {
                meaningsNode.forEach(m -> {
                    String meaning = m.asText().trim();
                    if (!meaning.isEmpty()) {
                        meanings.add(meaning);
                    }
                });
            }
            vocabulary.setMeaning(meanings.isEmpty() ? "Không có nghĩa" : String.join("; ", meanings));

            // Parse examples with validation
            List<String> examples = new ArrayList<>();
            JsonNode examplesNode = node.path("examples");
            if (examplesNode.isArray()) {
                examplesNode.forEach(e -> {
                    String example = e.asText().trim();
                    if (!example.isEmpty()) {
                        examples.add(example);
                    }
                });
            }
            vocabulary.setExample(examples.isEmpty() ? "No example" : String.join("\n", examples));

        } catch (Exception e) {
            log.error("Failed to parse AI response for word '{}': {}", vocabulary.getWord(), e.getMessage());
            log.debug("Raw AI response: {}", aiResponse);
            setFallbackData(vocabulary);
        }
    }

    private void setFallbackData(Vocabulary vocabulary) {
        vocabulary.setIpa("");
        vocabulary.setMeaning("Không thể lấy nghĩa từ AI");
        vocabulary.setWordType("unknown");
        vocabulary.setExample("No example available");
        vocabulary.setCefrLevel("B1");
    }

    /**
     * Lấy URL audio từ Free Dictionary API
     * API: https://api.dictionaryapi.dev/api/v2/entries/en/{word}
     */
    private String fetchAudioUrl(String word) {
        try {
            String apiUrl = "https://api.dictionaryapi.dev/api/v2/entries/en/" + word.toLowerCase();
            log.info("Fetching audio URL from: {}", apiUrl);

            String response = restTemplate.getForObject(apiUrl, String.class);
            JsonNode rootArray = objectMapper.readTree(response);

            if (rootArray.isArray() && rootArray.size() > 0) {
                JsonNode firstEntry = rootArray.get(0);
                JsonNode phonetics = firstEntry.path("phonetics");

                // Tìm audio URL đầu tiên có sẵn
                for (JsonNode phonetic : phonetics) {
                    String audio = phonetic.path("audio").asText("");
                    if (!audio.isEmpty()) {
                        log.info("Found audio URL: {}", audio);
                        return audio;
                    }
                }
            }

            log.warn("No audio found for word: {}", word);
            return null;
        } catch (Exception e) {
            log.error("Error fetching audio URL: {}", e.getMessage());
            return null;
        }
    }

    public ResultPaginationDTO handleGetAllVocabulary(String search, Pageable pageable) {
        // Build Specification for user and search filters
        Specification<Vocabulary> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by current user
            String email = SecurityUtil.getCurrentUserLogin().orElseThrow(
                    () -> new RuntimeException("User not authenticated"));
            User user = userRepository.findByEmail(email);
            if (user == null) {
                throw new RuntimeException("User not found");
            }
            predicates.add(criteriaBuilder.equal(root.get("user").get("id"), user.getId()));

            // Add search filter if provided
            if (search != null && !search.trim().isEmpty()) {
                String likePattern = "%" + search.toLowerCase() + "%";
                Predicate wordPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("word")), likePattern);
                Predicate meaningPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("meaning")), likePattern);
                Predicate cefrPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("cefrLevel")), likePattern);
                Predicate wordTypePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("wordType")), likePattern);

                predicates.add(criteriaBuilder.or(wordPredicate, meaningPredicate, cefrPredicate, wordTypePredicate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        // Execute query with spec
        Page<Vocabulary> pageVocabulary = this.vocabularyRepository.findAll(spec, pageable);

        // Build response
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageVocabulary.getTotalPages());
        mt.setTotal(pageVocabulary.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pageVocabulary.getContent().stream()
                .map(VocabularyDetailResponse::fromEntity)
                .toList());

        return rs;
    }

    /**
     * Preview vocabulary data without saving to database
     * Used for showing preview before adding word
     * Cached to avoid duplicate AI calls for same word
     */
    public VocabularyDetailResponse previewVocabulary(AddVocabularyRequest request) {
        String word = request.getWord();
        String cacheKey = word.toLowerCase().trim(); // Case-insensitive cache key

        // Check cache first
        VocabularyDetailResponse cachedResponse = previewCache.get(cacheKey);
        if (cachedResponse != null) {
            log.info("Cache HIT for preview: {}", word);
            return cachedResponse;
        }

        log.info("Cache MISS - Generating preview for: {}", word);

        // Create temporary entity (not saved to DB)
        Vocabulary vocabulary = new Vocabulary();
        vocabulary.setWord(word);

        // Enrich with AI (expensive operation)
        enrichVocabularyWithAI(vocabulary);

        // Get audio URL
        vocabulary.setAudioUrl(fetchAudioUrl(word));

        VocabularyDetailResponse response = VocabularyDetailResponse.fromEntity(vocabulary);

        // Cache the response for 30 minutes
        previewCache.put(cacheKey, response);
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
                .schedule(() -> {
                    previewCache.remove(cacheKey);
                    log.info("Preview cache expired for: {}", word);
                }, 30, java.util.concurrent.TimeUnit.MINUTES);

        log.info("Preview generated and cached for: {}", word);
        return response;
    }

}
