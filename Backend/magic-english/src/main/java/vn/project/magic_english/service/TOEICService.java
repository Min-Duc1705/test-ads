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
        boolean isListening = request.getSection().contains("Listening") || request.getSection().contains("Part 1")
                || request.getSection().contains("Part 2") || request.getSection().contains("Part 3")
                || request.getSection().contains("Part 4");

        if (isListening) {
            return buildListeningPrompt(request);
        }

        return buildReadingPrompt(request);
    }

    /**
     * Build AI prompt specifically for TOEIC Listening test
     * Following the standard TOEIC Listening format:
     * - Part 1: Photographs (5 questions)
     * - Part 2: Question & Response (5 questions)
     * - Part 3: Conversations (5 questions)
     * - Part 4: Talks (5 questions)
     * Total: 20 questions for shortened version
     */
    private String buildListeningPrompt(GenerateTOEICTestRequest request) {
        String difficulty = request.getDifficulty();

        // Shortened TOEIC Listening: 20 questions total (5 per part)
        int part1Questions = 5; // Photographs
        int part2Questions = 5; // Question & Response
        int part3Questions = 5; // Conversations (2-3 conversations, 1-3 questions each)
        int part4Questions = 5; // Talks (2-3 talks, 1-3 questions each)
        int totalQuestions = part1Questions + part2Questions + part3Questions + part4Questions;

        return String.format(
                """
                        Generate a TOEIC Listening Test with %s difficulty following the EXACT TOEIC format.

                        IMPORTANT: Generate EXACTLY %d questions total, divided into 4 Parts:

                        === PART 1 - PHOTOGRAPHS (Questions 1-%d) ===
                        Format: Describe a photograph scene in the "passage" field (30-50 words describing what's in the photo: people, actions, locations, objects).
                        Question: "Which statement best describes the photograph?"
                        Answers: 4 statements (A, B, C, D) describing the photo - only ONE is the BEST description.

                        Example passage: "A woman in business attire is standing at a podium, addressing a seated audience in a conference room. There are presentation slides visible on a screen behind her."
                        Example answers:
                        - A: "The woman is giving a presentation." (correct - most accurate)
                        - B: "People are leaving the room."
                        - C: "The screen is turned off."
                        - D: "Everyone is standing."

                        === PART 2 - QUESTION & RESPONSE (Questions %d-%d) ===
                        Format: A direct question in "questionText" field (workplace scenarios). NO passage needed.
                        Question: A question that would be asked in a workplace context.
                        Answers: 4 possible responses (A, B, C, D) - only ONE is the most appropriate response.

                        Example questionText: "When is the deadline for the quarterly report?"
                        Example answers:
                        - A: "By the end of this week." (correct - directly answers the question)
                        - B: "Yes, the report is ready."
                        - C: "I prefer working in the morning."
                        - D: "The meeting room is on the third floor."

                        === PART 3 - CONVERSATIONS (Questions %d-%d) ===
                        Format: A conversation between 2-3 people (100-150 words) in the "passage" field.
                        MUST use speaker labels like "Man:" "Woman:" or "Speaker 1:" "Speaker 2:" "Speaker 3:"
                        Multiple questions can share the same conversation.
                        Questions should ask about: main topic, details, speaker's intention, what will happen next.

                        Example passage: "Man: Have you finished reviewing the proposal?\nWoman: Almost. I just need to check the budget section.\nMan: The client wants it by tomorrow morning.\nWoman: I'll have it ready by tonight."

                        === PART 4 - TALKS (Questions %d-%d) ===
                        Format: A short announcement, speech, or instruction (100-150 words) in the "passage" field.
                        Types: meeting announcements, voicemail messages, news reports, tour guides, advertisements.
                        Multiple questions can share the same talk.
                        Questions should ask about: purpose, details, what listeners should do.

                        Example passage: "Attention all employees. Due to the building maintenance scheduled for this Saturday, the parking garage will be closed. Please use the street parking or public transportation. The maintenance is expected to be completed by Sunday evening."

                        === IMPORTANT RULES ===
                        1. Each question MUST have "part" field: "Part 1", "Part 2", "Part 3", or "Part 4"
                        2. "isCorrect" MUST be boolean (true or false), NOT string
                        3. For correct answers: Provide 2-3 sentence explanation WHY it's correct
                        4. For incorrect answers: Provide 1 sentence explanation WHY it's wrong
                        5. Use appropriate %s difficulty level vocabulary and scenarios
                        6. Return ONLY valid JSON (no markdown, no extra text)

                        Return JSON format:
                        {
                          "title": "TOEIC Listening Test - %s",
                          "durationMinutes": 25,
                          "questions": [
                            {
                              "questionNumber": 1,
                              "part": "Part 1",
                              "questionText": "Which statement best describes the photograph?",
                              "questionType": "photograph",
                              "passage": "Description of the photograph scene...",
                              "answers": [
                                {"answerOption": "A", "answerText": "Statement A", "isCorrect": true, "explanation": "Why correct"},
                                {"answerOption": "B", "answerText": "Statement B", "isCorrect": false, "explanation": "Why wrong"},
                                {"answerOption": "C", "answerText": "Statement C", "isCorrect": false, "explanation": "Why wrong"},
                                {"answerOption": "D", "answerText": "Statement D", "isCorrect": false, "explanation": "Why wrong"}
                              ]
                            }
                          ]
                        }
                        """,
                difficulty,
                totalQuestions,
                part1Questions,
                part1Questions + 1, part1Questions + part2Questions,
                part1Questions + part2Questions + 1, part1Questions + part2Questions + part3Questions,
                part1Questions + part2Questions + part3Questions + 1, totalQuestions,
                difficulty,
                difficulty);
    }

    /**
     * Build AI prompt for TOEIC Reading test
     * Following the standard TOEIC Reading format:
     * - Part 5: Incomplete Sentences (5 questions)
     * - Part 6: Text Completion (5 questions)
     * - Part 7: Reading Comprehension (5 questions)
     * Total: 15 questions for shortened version
     */
    private String buildReadingPrompt(GenerateTOEICTestRequest request) {
        String difficulty = request.getDifficulty();

        // Shortened TOEIC Reading: 15 questions total (5 per part)
        int part5Questions = 5; // Incomplete Sentences
        int part6Questions = 5; // Text Completion
        int part7Questions = 5; // Reading Comprehension
        int totalQuestions = part5Questions + part6Questions + part7Questions;

        return String.format(
                """
                        Generate a TOEIC Reading Test with %s difficulty following the EXACT TOEIC format.

                        IMPORTANT: Generate EXACTLY %d questions total, divided into 3 Parts:

                        === PART 5 - INCOMPLETE SENTENCES (Questions 1-%d) ===
                        Format: A single sentence with ONE blank (______) where a word or phrase should be inserted.
                        Question: The sentence with blank in "questionText" field. NO passage needed (set passage to null).
                        Answers: 4 word/phrase options (A, B, C, D) - only ONE grammatically and contextually correct.
                        Focus: Grammar (verb tenses, prepositions, conjunctions, articles) and vocabulary (word forms, collocations).

                        Example questionText: "The quarterly sales report must be ______ to the board of directors by Friday."
                        Example answers:
                        - A: "submit" (wrong - incorrect verb form)
                        - B: "submitted" (correct - passive voice past participle needed)
                        - C: "submitting" (wrong - gerund form inappropriate here)
                        - D: "submission" (wrong - noun form doesn't fit grammatically)

                        === PART 6 - TEXT COMPLETION (Questions %d-%d) ===
                        Format: A short business document (email, memo, notice, advertisement) with 2-3 blanks marked as [1], [2], [3].
                        The passage goes in "passage" field (80-120 words).
                        Each question asks what should fill a specific blank.
                        Multiple questions can share the same passage.
                        Answers: 4 options - can be single words, phrases, or complete sentences.

                        Example passage: "Dear Valued Customer,\\n\\nWe are pleased to announce that our new store location will open [1] Monday, March 15th. [2] To celebrate this occasion, we are offering a 20%% discount on all items during the opening week. [3]\\n\\nBest regards,\\nStore Management"
                        Example question: "What should be placed in blank [1]?"
                        Example answers:
                        - A: "on" (correct - correct preposition for days)
                        - B: "at" (wrong - used for specific times, not days)
                        - C: "in" (wrong - used for months/years, not specific dates with days)
                        - D: "by" (wrong - indicates deadline, not opening date)

                        === PART 7 - READING COMPREHENSION (Questions %d-%d) ===
                        Format: A longer business text (150-250 words) such as email, article, advertisement, letter, or memo.
                        The passage goes in "passage" field.
                        Multiple questions can share the same passage.
                        Questions ask about: main idea, specific details, inferences, vocabulary in context, purpose of the text.

                        Example passage: "MEMORANDUM\\n\\nTo: All Department Managers\\nFrom: Human Resources\\nDate: October 5\\nSubject: New Employee Training Program\\n\\nStarting next month, all new employees will be required to complete a comprehensive training program before beginning their regular duties. The program will consist of three modules: company policies, safety procedures, and job-specific skills. Each module will take approximately two days to complete.\\n\\nDepartment managers are requested to submit their new hire schedules to HR by October 15 so that training sessions can be arranged accordingly."
                        Example questions:
                        - "What is the purpose of this memo?" (main idea)
                        - "How long will the complete training program take?" (specific detail)
                        - "What are department managers asked to do?" (specific detail)

                        === IMPORTANT RULES ===
                        1. Each question MUST have "part" field: "Part 5", "Part 6", or "Part 7"
                        2. "isCorrect" MUST be boolean (true or false), NOT string
                        3. For correct answers: Provide 2-3 sentence explanation WHY it's correct
                        4. For incorrect answers: Provide 1 sentence explanation WHY it's wrong
                        5. Use appropriate %s difficulty level vocabulary and business scenarios
                        6. Part 5: passage should be null (just sentence in questionText)
                        7. Part 6 & 7: passage is required, multiple questions can share same passage
                        8. Return ONLY valid JSON (no markdown, no extra text)

                        Return JSON format:
                        {
                          "title": "TOEIC Reading Test - %s",
                          "durationMinutes": 25,
                          "questions": [
                            {
                              "questionNumber": 1,
                              "part": "Part 5",
                              "questionText": "Sentence with ______ blank here.",
                              "questionType": "incomplete_sentence",
                              "passage": null,
                              "answers": [
                                {"answerOption": "A", "answerText": "Option A", "isCorrect": false, "explanation": "Why wrong"},
                                {"answerOption": "B", "answerText": "Option B", "isCorrect": true, "explanation": "Why correct"},
                                {"answerOption": "C", "answerText": "Option C", "isCorrect": false, "explanation": "Why wrong"},
                                {"answerOption": "D", "answerText": "Option D", "isCorrect": false, "explanation": "Why wrong"}
                              ]
                            },
                            {
                              "questionNumber": 6,
                              "part": "Part 6",
                              "questionText": "What should be placed in blank [1]?",
                              "questionType": "text_completion",
                              "passage": "Business document with [1] blanks [2] here...",
                              "answers": [...]
                            },
                            {
                              "questionNumber": 11,
                              "part": "Part 7",
                              "questionText": "What is the main purpose of this email?",
                              "questionType": "reading_comprehension",
                              "passage": "Full business document here...",
                              "answers": [...]
                            }
                          ]
                        }
                        """,
                difficulty,
                totalQuestions,
                part5Questions,
                part5Questions + 1, part5Questions + part6Questions,
                part5Questions + part6Questions + 1, totalQuestions,
                difficulty,
                difficulty);
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

            // Parse part field for Listening tests (Part 1, Part 2, Part 3, Part 4)
            if (questionNode.has("part") && !questionNode.get("part").isNull()) {
                String partValue = questionNode.get("part").asText();
                question.setPart(partValue);
                // Also set the test's part if not already set
                if (test.getPart() == null) {
                    test.setPart("Listening"); // Mark as Listening test with multiple parts
                }

                // Generate image for Part 1 - Photographs using Pollinations AI (free, no API
                // key needed)
                if (partValue.toLowerCase().contains("part 1") && passage != null && !passage.isEmpty()) {
                    try {
                        // Use Pollinations AI to generate image based on passage description
                        // Format: https://pollinations.ai/p/{encoded_prompt}
                        String imagePrompt = "TOEIC test photograph, realistic business workplace scene: " + passage;
                        String encodedPrompt = java.net.URLEncoder.encode(imagePrompt, "UTF-8");
                        String imageUrl = "https://image.pollinations.ai/prompt/" + encodedPrompt
                                + "?width=400&height=300&nologo=true";
                        question.setImageUrl(imageUrl);
                        log.info("Generated image URL for TOEIC Part 1 question {}: {}", question.getQuestionNumber(),
                                imageUrl);
                    } catch (Exception e) {
                        log.warn("Failed to generate image for Part 1 question {}: {}", question.getQuestionNumber(),
                                e.getMessage());
                    }
                }
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
                    qr.setPart(q.getPart()); // Set Part for Listening questions
                    qr.setImageUrl(q.getImageUrl()); // Set Image URL for Part 1 Photographs

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
