package vn.project.magic_english.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.project.magic_english.model.*;
import vn.project.magic_english.model.request.GenerateTOEICTestRequest;
import vn.project.magic_english.model.request.StartTOEICTestRequest;
import vn.project.magic_english.model.request.SubmitTOEICTestRequest;
import vn.project.magic_english.model.response.*;
import vn.project.magic_english.repository.*;
import vn.project.magic_english.utils.SecurityUtil;
import vn.project.magic_english.utils.error.IdInvalidException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TOEICService {

    private final AiClientService aiClientService; // Changed from ChatClient
    private final TOEICTestRepository testRepository;
    private final TOEICTestHistoryRepository historyRepository;
    private final TOEICUserAnswerRepository userAnswerRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final TextToSpeechService textToSpeechService;

    /**
     * Generate TOEIC test using Gemini AI
     */
    @Transactional
    public TOEICTestResponse generateTest(GenerateTOEICTestRequest request) throws JsonProcessingException {
        String aiPrompt = buildAIPrompt(request);
        String aiResponse = generateWithAI(aiPrompt);

        TOEICTest test = parseAndSaveTest(aiResponse, request);
        return convertToResponse(test, false);
    }

    /**
     * Start a test session
     */
    @Transactional
    public TOEICTestHistoryResponse startTest(StartTOEICTestRequest request) throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(
                () -> new IdInvalidException("User not authenticated"));
        User user = userRepository.findByEmail(email);

        TOEICTest test = testRepository.findById(request.getTestId())
                .orElseThrow(() -> new IdInvalidException("Test not found"));

        TOEICTestHistory history = new TOEICTestHistory();
        history.setUser(user);
        history.setTest(test);
        history.setStatus("in_progress");
        history.setStartedAt(Instant.now());
        history.setTotalAnswers(test.getTotalQuestions());
        historyRepository.save(history);

        return convertToHistoryResponse(history);
    }

    /**
     * Submit test and calculate TOEIC score
     */
    @Transactional
    public TOEICTestResultResponse submitTest(SubmitTOEICTestRequest request) throws IdInvalidException {
        TOEICTestHistory history = historyRepository.findById(request.getHistoryId())
                .orElseThrow(() -> new IdInvalidException("Test history not found"));

        if (!"in_progress".equals(history.getStatus())) {
            throw new IdInvalidException("Test is already completed");
        }

        int correctAnswers = 0;
        List<TOEICQuestionResultResponse> questionResults = new ArrayList<>();

        for (Map<String, Object> answerMap : request.getAnswers()) {
            Long questionId = ((Number) answerMap.get("questionId")).longValue();
            Long selectedAnswerId = answerMap.get("selectedAnswerId") != null
                    ? ((Number) answerMap.get("selectedAnswerId")).longValue()
                    : null;

            TOEICQuestion question = history.getTest().getQuestions().stream()
                    .filter(q -> q.getId().equals(questionId))
                    .findFirst()
                    .orElseThrow(() -> new IdInvalidException("Question not found"));

            TOEICAnswer correctAnswer = question.getAnswers().stream()
                    .filter(TOEICAnswer::getIsCorrect)
                    .findFirst()
                    .orElse(null);

            boolean isCorrect = false;
            TOEICAnswer selectedAnswer = null;

            if (selectedAnswerId != null) {
                selectedAnswer = question.getAnswers().stream()
                        .filter(a -> a.getId().equals(selectedAnswerId))
                        .findFirst()
                        .orElse(null);

                if (selectedAnswer != null) {
                    isCorrect = selectedAnswer.getIsCorrect();
                }
            }

            if (isCorrect) {
                correctAnswers++;
            }

            // Save user answer
            TOEICUserAnswer userAnswer = new TOEICUserAnswer();
            userAnswer.setHistory(history);
            userAnswer.setQuestion(question);
            userAnswer.setSelectedAnswer(selectedAnswer);
            userAnswer.setIsCorrect(isCorrect);
            userAnswerRepository.save(userAnswer);

            // Build question result
            TOEICQuestionResultResponse resultResponse = new TOEICQuestionResultResponse();
            resultResponse.setQuestionId(question.getId());
            resultResponse.setQuestionNumber(question.getQuestionNumber());
            resultResponse.setQuestionText(question.getQuestionText());
            resultResponse.setPassage(question.getPassage());
            resultResponse.setAudioUrl(question.getAudioUrl());
            resultResponse.setIsCorrect(isCorrect);

            if (selectedAnswer != null) {
                resultResponse.setSelectedAnswerId(selectedAnswer.getId());
                resultResponse.setSelectedAnswerOption(selectedAnswer.getAnswerOption());
                resultResponse.setSelectedAnswerText(selectedAnswer.getAnswerText());
            }

            if (correctAnswer != null) {
                resultResponse.setCorrectAnswerId(correctAnswer.getId());
                resultResponse.setCorrectAnswerOption(correctAnswer.getAnswerOption());
                resultResponse.setCorrectAnswerText(correctAnswer.getAnswerText());
                resultResponse.setExplanation(correctAnswer.getExplanation());
            }

            questionResults.add(resultResponse);
        }

        // Calculate TOEIC score (0-990)
        double percentage = (double) correctAnswers / history.getTotalAnswers();
        int score = calculateTOEICScore(percentage, history.getTotalAnswers());

        // Update history
        history.setCorrectAnswers(correctAnswers);
        history.setScore(score);
        history.setStatus("completed");
        history.setCompletedAt(Instant.now());
        history.setTimeSpentSeconds(request.getTimeSpentSeconds());
        historyRepository.save(history);

        // Build result
        TOEICTestResultResponse result = new TOEICTestResultResponse();
        result.setHistoryId(history.getId());
        result.setTestId(history.getTest().getId());
        result.setTestTitle(history.getTest().getTitle());
        result.setSection(history.getTest().getSection());
        result.setDifficulty(history.getTest().getDifficulty());
        result.setScore(score);
        result.setCorrectAnswers(correctAnswers);
        result.setTotalAnswers(history.getTotalAnswers());
        result.setAccuracyPercentage(percentage * 100);
        result.setTimeSpentSeconds(request.getTimeSpentSeconds());
        result.setCompletedAt(history.getCompletedAt());
        result.setQuestionResults(questionResults);

        return result;
    }

    /**
     * Get user test history
     */
    public List<TOEICTestHistoryResponse> getUserHistory() throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(
                () -> new IdInvalidException("User not authenticated"));
        User user = userRepository.findByEmail(email);

        return historyRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::convertToHistoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get test by ID
     */
    public TOEICTestResponse getTestById(Long testId) throws IdInvalidException {
        TOEICTest test = testRepository.findById(testId)
                .orElseThrow(() -> new IdInvalidException("Test not found"));
        return convertToResponse(test, false);
    }

    // ========== Private Helper Methods ==========

    private String buildAIPrompt(GenerateTOEICTestRequest request) {
        int numQuestions = request.getDifficulty().equals("Easy") ? 10
                : request.getDifficulty().equals("Medium") ? 15 : 20;

        boolean isListening = request.getSection().contains("Listening") || request.getSection().contains("Part 1")
                || request.getSection().contains("Part 2") || request.getSection().contains("Part 3")
                || request.getSection().contains("Part 4");

        String sectionGuidance = isListening
                ? "For Listening: Create short business English conversations or announcements (100-150 words each) in the 'passage' field. Use natural spoken business English. IMPORTANT: You MUST use 'Man:' and 'Woman:' labels for conversations to indicate different speakers. Focus on workplace scenarios: meetings, phone calls, announcements, instructions."
                : "For Reading: Include business documents, emails, advertisements, or articles with comprehension questions. Use realistic business scenarios.";

        return String.format(
                """
                        Generate a TOEIC %s test with %s difficulty.

                        Create %d multiple choice questions. Each question has 4 options (A, B, C, D) with ONE correct answer.

                        %s

                        IMPORTANT:
                        - For correct answers: Provide 2-3 sentence explanation WHY it's correct
                        - For incorrect answers: Provide 1 sentence explanation WHY it's wrong
                        - Use business English vocabulary and realistic workplace scenarios
                        - "isCorrect" MUST be boolean (true or false), NOT string
                        - All field values must match their expected types exactly

                        Return ONLY a valid JSON object (no markdown, no extra text):
                        {
                          "title": "TOEIC %s Test - %s",
                          "durationMinutes": 45,
                          "questions": [
                            {
                              "questionNumber": 1,
                              "questionText": "Question text here",
                              "questionType": "multiple_choice",
                              "passage": "For Listening: Natural spoken business English (100-150 words with speaker labels). For Reading: Business document or email. Can be null for grammar questions.",
                              "answers": [
                                {"answerOption": "A", "answerText": "Option A", "isCorrect": false, "explanation": "Why wrong"},
                                {"answerOption": "B", "answerText": "Option B", "isCorrect": true, "explanation": "Why correct"},
                                {"answerOption": "C", "answerText": "Option C", "isCorrect": false, "explanation": "Why wrong"},
                                {"answerOption": "D", "answerText": "Option D", "isCorrect": false, "explanation": "Why wrong"}
                              ]
                            }
                          ]
                        }
                        """,
                request.getSection(),
                request.getDifficulty(),
                numQuestions,
                sectionGuidance,
                request.getSection(),
                request.getDifficulty());
    }

    private String generateWithAI(String prompt) {
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                log.info("Attempting to generate test with AI (attempt {}/{})", retryCount + 1, maxRetries);

                // Using AiClientService for rotation
                String response = aiClientService.generate(prompt);

                log.info("AI Response received successfully, length: {}", response.length());
                return response;
            } catch (Exception e) {
                retryCount++;
                log.error("Failed to generate test with AI (attempt {}/{}): {}", retryCount, maxRetries,
                        e.getMessage());

                if (retryCount >= maxRetries) {
                    throw new RuntimeException(
                            "Failed to generate test after " + maxRetries + " attempts: " + e.getMessage());
                }

                // Wait before retry (exponential backoff)
                try {
                    long waitTime = (long) Math.pow(2, retryCount) * 1000; // 2s, 4s, 8s
                    log.info("Waiting {}ms before retry...", waitTime);
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted: " + ie.getMessage());
                }
            }
        }

        throw new RuntimeException("Failed to generate test after all retries");
    }

    private TOEICTest parseAndSaveTest(String aiResponse, GenerateTOEICTestRequest request)
            throws JsonProcessingException {
        String cleanedResponse = aiResponse.trim();
        if (cleanedResponse.startsWith("```json")) {
            cleanedResponse = cleanedResponse.substring(7);
        }
        if (cleanedResponse.startsWith("```")) {
            cleanedResponse = cleanedResponse.substring(3);
        }
        if (cleanedResponse.endsWith("```")) {
            cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
        }
        cleanedResponse = cleanedResponse.trim();

        JsonNode rootNode = objectMapper.readTree(cleanedResponse);

        TOEICTest test = new TOEICTest();
        test.setSection(request.getSection());
        test.setDifficulty(request.getDifficulty());
        test.setTitle(rootNode.get("title").asText());
        test.setDurationMinutes(rootNode.get("durationMinutes").asInt());
        test.setTotalQuestions(rootNode.get("questions").size());

        boolean isListening = request.getSection().contains("Listening") || request.getSection().contains("Part 1")
                || request.getSection().contains("Part 2") || request.getSection().contains("Part 3")
                || request.getSection().contains("Part 4");

        JsonNode questionsNode = rootNode.get("questions");
        for (JsonNode questionNode : questionsNode) {
            TOEICQuestion question = new TOEICQuestion();
            question.setTest(test);
            question.setQuestionNumber(questionNode.get("questionNumber").asInt());
            question.setQuestionText(questionNode.get("questionText").asText());
            question.setQuestionType(
                    questionNode.has("questionType") ? questionNode.get("questionType").asText() : "multiple_choice");

            String passage = null;
            if (questionNode.has("passage") && !questionNode.get("passage").isNull()) {
                passage = questionNode.get("passage").asText();
                question.setPassage(passage);
            }

            // Generate audio for Listening tests
            if (isListening && passage != null && !passage.isEmpty()) {
                String textToConvert = passage;
                String audioUrl = textToSpeechService.generateWithResponsiveVoice(textToConvert);
                question.setAudioUrl(audioUrl);
                log.info("Generated audio URL for TOEIC Listening question {}: {}", question.getQuestionNumber(),
                        audioUrl);
            }

            JsonNode answersNode = questionNode.get("answers");
            for (JsonNode answerNode : answersNode) {
                TOEICAnswer answer = new TOEICAnswer();
                answer.setQuestion(question);
                answer.setAnswerOption(answerNode.get("answerOption").asText());
                answer.setAnswerText(answerNode.get("answerText").asText());

                // Handle isCorrect as both boolean and string with null check
                JsonNode isCorrectNode = answerNode.get("isCorrect");
                boolean isCorrect = false;
                if (isCorrectNode != null && !isCorrectNode.isNull()) {
                    if (isCorrectNode.isBoolean()) {
                        isCorrect = isCorrectNode.asBoolean();
                    } else if (isCorrectNode.isTextual()) {
                        String value = isCorrectNode.asText().toLowerCase().trim();
                        isCorrect = "true".equals(value) || "yes".equals(value) || "1".equals(value);
                    } else if (isCorrectNode.isNumber()) {
                        isCorrect = isCorrectNode.asInt() == 1;
                    }
                }
                answer.setIsCorrect(isCorrect);

                if (answerNode.has("explanation") && !answerNode.get("explanation").isNull()) {
                    answer.setExplanation(answerNode.get("explanation").asText());
                }
                question.getAnswers().add(answer);
            }

            test.getQuestions().add(question);
        }

        return testRepository.save(test);
    }

    private int calculateTOEICScore(double percentage, int totalQuestions) {
        // TOEIC scoring: 0-990 (10-495 per section)
        // Simplified conversion
        int baseScore = (int) (percentage * 495);
        if (totalQuestions >= 100) {
            // Full test (Listening + Reading)
            return (int) (percentage * 990);
        } else {
            // Single section
            return Math.max(10, Math.min(495, 10 + baseScore));
        }
    }

    private TOEICTestResponse convertToResponse(TOEICTest test, boolean includeCorrectAnswers) {
        TOEICTestResponse response = new TOEICTestResponse();
        response.setId(test.getId());
        response.setSection(test.getSection());
        response.setPart(test.getPart());
        response.setDifficulty(test.getDifficulty());
        response.setTitle(test.getTitle());
        response.setDurationMinutes(test.getDurationMinutes());
        response.setTotalQuestions(test.getTotalQuestions());

        List<TOEICQuestionResponse> questions = test.getQuestions().stream()
                .map(q -> {
                    TOEICQuestionResponse qr = new TOEICQuestionResponse();
                    qr.setId(q.getId());
                    qr.setQuestionNumber(q.getQuestionNumber());
                    qr.setQuestionText(q.getQuestionText());
                    qr.setQuestionType(q.getQuestionType());
                    qr.setPassage(q.getPassage());
                    qr.setAudioUrl(q.getAudioUrl());

                    List<TOEICAnswerResponse> answers = q.getAnswers().stream()
                            .map(a -> {
                                TOEICAnswerResponse ar = new TOEICAnswerResponse();
                                ar.setId(a.getId());
                                ar.setAnswerOption(a.getAnswerOption());
                                ar.setAnswerText(a.getAnswerText());
                                ar.setIsCorrect(includeCorrectAnswers ? a.getIsCorrect() : null);
                                ar.setExplanation(includeCorrectAnswers ? a.getExplanation() : null);
                                return ar;
                            })
                            .collect(Collectors.toList());
                    qr.setAnswers(answers);
                    return qr;
                })
                .collect(Collectors.toList());

        response.setQuestions(questions);
        return response;
    }

    private TOEICTestHistoryResponse convertToHistoryResponse(TOEICTestHistory history) {
        TOEICTestHistoryResponse response = new TOEICTestHistoryResponse();
        response.setId(history.getId());
        response.setTestId(history.getTest().getId());
        response.setTestTitle(history.getTest().getTitle());
        response.setSection(history.getTest().getSection());
        response.setDifficulty(history.getTest().getDifficulty());
        response.setStartedAt(history.getStartedAt());
        response.setCompletedAt(history.getCompletedAt());
        response.setScore(history.getScore());
        response.setCorrectAnswers(history.getCorrectAnswers());
        response.setTotalAnswers(history.getTotalAnswers());
        response.setStatus(history.getStatus());
        response.setTimeSpentSeconds(history.getTimeSpentSeconds());
        return response;
    }
}
