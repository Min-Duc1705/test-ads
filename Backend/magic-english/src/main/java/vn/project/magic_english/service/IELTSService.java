package vn.project.magic_english.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.project.magic_english.model.*;
import vn.project.magic_english.model.request.GenerateIELTSTestRequest;
import vn.project.magic_english.model.request.StartIELTSTestRequest;
import vn.project.magic_english.model.request.SubmitIELTSTestRequest;
import vn.project.magic_english.model.response.*;
import vn.project.magic_english.repository.*;
import vn.project.magic_english.utils.SecurityUtil;
import vn.project.magic_english.utils.error.IdInvalidException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IELTSService {

    private final AiClientService aiClientService;
    private final IELTSTestRepository testRepository;
    private final IELTSTestHistoryRepository historyRepository;
    private final IELTSUserAnswerRepository userAnswerRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final TextToSpeechService textToSpeechService;

    /**
     * Generate IELTS test using Gemini AI
     * Always generates a new test to avoid caching issues
     */
    @Transactional
    public IELTSTestResponse generateTest(GenerateIELTSTestRequest request) throws JsonProcessingException {
        // Always generate new test using AI (no caching)
        String aiPrompt = buildAIPrompt(request);
        String aiResponse = generateWithAI(aiPrompt);

        // Parse AI response and save to database
        IELTSTest test = parseAndSaveTest(aiResponse, request);

        return convertToResponse(test, false);
    }

    /**
     * Start a test session
     */
    @Transactional
    public IELTSTestHistoryResponse startTest(StartIELTSTestRequest request) throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(
                () -> new IdInvalidException("User not authenticated"));
        User user = userRepository.findByEmail(email);

        IELTSTest test = testRepository.findById(request.getTestId())
                .orElseThrow(() -> new IdInvalidException("Test not found with ID: " + request.getTestId()));

        IELTSTestHistory history = new IELTSTestHistory();
        history.setUser(user);
        history.setTest(test);
        history.setStatus("in_progress");
        history.setStartedAt(Instant.now());
        history.setTotalAnswers(test.getTotalQuestions());

        historyRepository.save(history);

        return convertToHistoryResponse(history);
    }

    /**
     * Submit test and calculate score
     */
    @Transactional
    public IELTSTestResultResponse submitTest(SubmitIELTSTestRequest request) throws IdInvalidException {
        IELTSTestHistory history = historyRepository.findById(request.getHistoryId())
                .orElseThrow(() -> new IdInvalidException("Test history not found"));

        if (!"in_progress".equals(history.getStatus())) {
            throw new IdInvalidException("Test is already completed");
        }

        boolean isWritingTest = "Writing".equalsIgnoreCase(history.getTest().getSkill());
        int correctAnswers = 0;
        double totalEssayScore = 0;
        List<IELTSQuestionResultResponse> questionResults = new ArrayList<>();

        // Process each answer
        for (SubmitIELTSTestRequest.UserAnswerRequest answerReq : request.getAnswers()) {
            IELTSQuestion question = history.getTest().getQuestions().stream()
                    .filter(q -> q.getId().equals(answerReq.getQuestionId()))
                    .findFirst()
                    .orElseThrow(() -> new IdInvalidException("Question not found"));

            IELTSQuestionResultResponse resultResponse = new IELTSQuestionResultResponse();
            resultResponse.setQuestionId(question.getId());
            resultResponse.setQuestionNumber(question.getQuestionNumber());
            resultResponse.setQuestionText(question.getQuestionText());

            if ("essay".equalsIgnoreCase(question.getQuestionType())) {
                // Handle Essay question - Grade with AI
                String userEssay = answerReq.getAnswerText();
                EssayGradingResult grading = gradeEssayWithAI(
                        question.getQuestionText(),
                        userEssay,
                        question.getSampleAnswer(),
                        question.getMinWords() != null ? question.getMinWords() : 150);

                totalEssayScore += grading.bandScore;
                resultResponse.setUserAnswer(userEssay);
                resultResponse.setCorrectAnswer(question.getSampleAnswer());
                resultResponse.setIsCorrect(grading.bandScore >= 6.0); // Consider 6.0+ as "correct"
                resultResponse.setExplanation(grading.feedback);

                // Save user answer
                IELTSUserAnswer userAnswer = new IELTSUserAnswer();
                userAnswer.setHistory(history);
                userAnswer.setQuestion(question);
                userAnswer.setUserAnswerText(userEssay);
                userAnswer.setIsCorrect(grading.bandScore >= 6.0);
                userAnswerRepository.save(userAnswer);

                if (grading.bandScore >= 6.0) {
                    correctAnswers++;
                }
            } else {
                // Handle Multiple Choice and Fill-in-blank type questions
                IELTSAnswer correctAnswer = question.getAnswers().stream()
                        .filter(IELTSAnswer::getIsCorrect)
                        .findFirst()
                        .orElse(null);

                boolean isCorrect = false;
                String userAnswerText = "";

                // Check if this is a fill-in-blank type (form_completion, sentence_completion,
                // flowchart, matching)
                if (answerReq.getAnswerText() != null && !answerReq.getAnswerText().trim().isEmpty()) {
                    // Fill-in-blank types: user typed an answer
                    userAnswerText = answerReq.getAnswerText().trim();

                    // Special handling for matching questions
                    if ("matching".equalsIgnoreCase(question.getQuestionType())) {
                        // Format: "0:A,1:B,2:C,3:D" - studentIndex:selectedLetter
                        int matchCorrect = 0;
                        int totalMatches = 0;
                        String[] pairs = userAnswerText.split(",");
                        for (String pair : pairs) {
                            String[] parts = pair.split(":");
                            if (parts.length == 2) {
                                int studentIndex = Integer.parseInt(parts[0].trim());
                                String selectedLetter = parts[1].trim();
                                totalMatches++;

                                // Check if this student's answer matches correct option
                                if (studentIndex < question.getAnswers().size()) {
                                    IELTSAnswer studentAnswer = question.getAnswers().get(studentIndex);
                                    if (selectedLetter.equalsIgnoreCase(studentAnswer.getAnswerOption())) {
                                        matchCorrect++;
                                    }
                                }
                            }
                        }
                        // Consider correct if all matches are correct
                        isCorrect = (matchCorrect == totalMatches && totalMatches > 0);
                    } else if ("flowchart".equalsIgnoreCase(question.getQuestionType())) {
                        // Format: "0:answer1,1:answer2,..." - blankIndex:userAnswer
                        int flowCorrect = 0;
                        int totalBlanks = 0;
                        String[] pairs = userAnswerText.split(",");
                        for (String pair : pairs) {
                            String[] parts = pair.split(":", 2); // Split only on first colon
                            if (parts.length == 2) {
                                int blankIndex = Integer.parseInt(parts[0].trim());
                                String userAnswer = parts[1].trim();
                                totalBlanks++;

                                // Check if this blank's answer matches correct answer
                                if (blankIndex < question.getAnswers().size()) {
                                    IELTSAnswer correctAns = question.getAnswers().get(blankIndex);
                                    if (userAnswer.equalsIgnoreCase(correctAns.getAnswerText().trim())) {
                                        flowCorrect++;
                                    }
                                }
                            }
                        }
                        // Consider correct if all blanks are correct
                        isCorrect = (flowCorrect == totalBlanks && totalBlanks > 0);
                    } else {
                        // Check correctness by comparing with correct answer text (case-insensitive)
                        if (correctAnswer != null && correctAnswer.getAnswerText() != null) {
                            isCorrect = userAnswerText.equalsIgnoreCase(correctAnswer.getAnswerText().trim());
                        }
                    }
                } else if (answerReq.getSelectedAnswerIds() != null && !answerReq.getSelectedAnswerIds().isEmpty()) {
                    // MCQ Multi-select: user selected multiple answers
                    List<Long> selectedIds = answerReq.getSelectedAnswerIds();

                    // Get all correct answer IDs
                    Set<Long> correctIds = question.getAnswers().stream()
                            .filter(IELTSAnswer::getIsCorrect)
                            .map(IELTSAnswer::getId)
                            .collect(Collectors.toSet());

                    // Get selected answer IDs as set
                    Set<Long> selectedIdsSet = new HashSet<>(selectedIds);

                    // Check if user selected exactly the correct answers
                    // Correct if: all selected are correct AND all correct are selected
                    isCorrect = selectedIdsSet.equals(correctIds);

                    // Build user answer text
                    userAnswerText = question.getAnswers().stream()
                            .filter(a -> selectedIds.contains(a.getId()))
                            .map(IELTSAnswer::getAnswerOption)
                            .sorted()
                            .collect(Collectors.joining(", "));
                } else if (answerReq.getSelectedAnswerId() != null) {
                    // MCQ Legacy (single select): user selected one answer
                    IELTSAnswer selectedAnswer = question.getAnswers().stream()
                            .filter(a -> a.getId().equals(answerReq.getSelectedAnswerId()))
                            .findFirst()
                            .orElse(null);

                    if (selectedAnswer != null) {
                        isCorrect = selectedAnswer.getIsCorrect();
                        userAnswerText = selectedAnswer.getAnswerOption();
                    }
                }

                if (isCorrect) {
                    correctAnswers++;
                }

                // Save user answer
                IELTSUserAnswer userAnswer = new IELTSUserAnswer();
                userAnswer.setHistory(history);
                userAnswer.setQuestion(question);
                if (answerReq.getSelectedAnswerId() != null) {
                    IELTSAnswer selectedAnswer = question.getAnswers().stream()
                            .filter(a -> a.getId().equals(answerReq.getSelectedAnswerId()))
                            .findFirst()
                            .orElse(null);
                    userAnswer.setSelectedAnswer(selectedAnswer);
                }
                userAnswer.setUserAnswerText(answerReq.getAnswerText());
                userAnswer.setIsCorrect(isCorrect);
                userAnswerRepository.save(userAnswer);

                resultResponse.setUserAnswer(userAnswerText);
                resultResponse.setIsCorrect(isCorrect);

                // For matching and flowchart questions, combine all answers and explanations
                if ("matching".equalsIgnoreCase(question.getQuestionType()) ||
                        "flowchart".equalsIgnoreCase(question.getQuestionType())) {
                    StringBuilder allAnswers = new StringBuilder();
                    StringBuilder allExplanations = new StringBuilder();
                    for (IELTSAnswer ans : question.getAnswers()) {
                        // Build all correct answers
                        if (allAnswers.length() > 0) {
                            allAnswers.append("|||");
                        }
                        allAnswers.append(ans.getAnswerOption())
                                .append(":")
                                .append(ans.getAnswerText());

                        // Build all explanations
                        if (ans.getExplanation() != null && !ans.getExplanation().isEmpty()) {
                            if (allExplanations.length() > 0) {
                                allExplanations.append("|||");
                            }
                            allExplanations.append(ans.getAnswerOption())
                                    .append(":")
                                    .append(ans.getAnswerText())
                                    .append(":")
                                    .append(ans.getExplanation());
                        }
                    }
                    resultResponse.setCorrectAnswer(allAnswers.toString());
                    resultResponse.setExplanation(allExplanations.toString());
                } else if ("multiple_choice".equalsIgnoreCase(question.getQuestionType())) {
                    // For MCQ (including multi-select), show correct option letters and combine
                    // explanations
                    StringBuilder correctLetters = new StringBuilder();
                    StringBuilder allExplanations = new StringBuilder();
                    for (IELTSAnswer ans : question.getAnswers()) {
                        if (ans.getIsCorrect()) {
                            if (correctLetters.length() > 0) {
                                correctLetters.append(", ");
                            }
                            correctLetters.append(ans.getAnswerOption());

                            // Combine explanations
                            if (ans.getExplanation() != null && !ans.getExplanation().isEmpty()) {
                                if (allExplanations.length() > 0) {
                                    allExplanations.append("\n");
                                }
                                allExplanations.append(ans.getExplanation());
                            }
                        }
                    }
                    resultResponse.setCorrectAnswer(correctLetters.toString());
                    resultResponse.setExplanation(allExplanations.length() > 0 ? allExplanations.toString() : null);
                } else {
                    resultResponse.setCorrectAnswer(correctAnswer != null ? correctAnswer.getAnswerText() : "");
                    resultResponse.setExplanation(correctAnswer != null ? correctAnswer.getExplanation() : "");
                }
            }

            questionResults.add(resultResponse);
        }

        // Calculate score
        BigDecimal score;
        if (isWritingTest && history.getTotalAnswers() > 0) {
            // For Writing: use average essay band score directly
            score = BigDecimal.valueOf(totalEssayScore / history.getTotalAnswers())
                    .setScale(1, RoundingMode.HALF_UP);
        } else {
            // For other skills: calculate from percentage
            double percentage = (double) correctAnswers / history.getTotalAnswers();
            score = calculateIELTSBandScore(percentage);
        }

        // Update history
        history.setCorrectAnswers(correctAnswers);
        history.setScore(score);
        history.setStatus("completed");
        history.setCompletedAt(Instant.now());
        history.setTimeSpentSeconds(request.getTimeSpentSeconds());
        historyRepository.save(history);

        // Build result response
        IELTSTestResultResponse result = new IELTSTestResultResponse();
        result.setHistoryId(history.getId());
        result.setScore(score.doubleValue());
        result.setCorrectAnswers(correctAnswers);
        result.setTotalQuestions(history.getTotalAnswers());
        result.setTimeSpentSeconds(request.getTimeSpentSeconds());
        result.setQuestionResults(questionResults);

        return result;
    }

    /**
     * Grade essay using AI
     */
    private EssayGradingResult gradeEssayWithAI(String topic, String userEssay, String sampleAnswer, int minWords) {
        if (userEssay == null || userEssay.trim().isEmpty()) {
            return new EssayGradingResult(0.0, "No essay submitted.");
        }

        int wordCount = userEssay.trim().split("\\s+").length;

        String prompt = String.format(
                """
                        You are an IELTS Writing examiner. Grade the following essay based on IELTS Writing Task 2 criteria.

                        TOPIC: %s

                        STUDENT'S ESSAY:
                        %s

                        REFERENCE SAMPLE ANSWER:
                        %s

                        WORD COUNT: %d words (minimum required: %d words)

                        Grade this essay on a scale of 1-9 (IELTS band score) considering:
                        1. Task Achievement (25%%): Does the essay address the topic fully?
                        2. Coherence & Cohesion (25%%): Is it well-organized with clear paragraphs?
                        3. Lexical Resource (25%%): Is vocabulary appropriate and varied?
                        4. Grammatical Range & Accuracy (25%%): Is grammar correct and varied?

                        Return ONLY a valid JSON object (no markdown):
                        {
                          "bandScore": 6.5,
                          "taskAchievement": 6.5,
                          "coherence": 6.0,
                          "lexicalResource": 7.0,
                          "grammar": 6.5,
                          "feedback": "Detailed feedback in 2-3 sentences explaining strengths and areas for improvement."
                        }
                        """,
                topic, userEssay, sampleAnswer != null ? sampleAnswer : "N/A", wordCount, minWords);

        try {
            String response = aiClientService.generate(prompt);

            // Clean response
            String cleanedResponse = response.trim();
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7);
            }
            if (cleanedResponse.startsWith("```")) {
                cleanedResponse = cleanedResponse.substring(3);
            }
            if (cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
            }

            JsonNode node = objectMapper.readTree(cleanedResponse.trim());
            double bandScore = node.get("bandScore").asDouble();
            String feedback = node.get("feedback").asText();

            // Penalize for word count below minimum
            if (wordCount < minWords) {
                bandScore = Math.max(1.0, bandScore - 1.0);
                feedback = String.format("Word count: %d/%d (below minimum). %s", wordCount, minWords, feedback);
            }

            return new EssayGradingResult(bandScore, feedback);
        } catch (Exception e) {
            log.error("Error grading essay with AI", e);
            // Fallback: basic word count based scoring
            double basicScore = Math.min(6.0, 3.0 + (wordCount / 50.0));
            return new EssayGradingResult(basicScore,
                    "Unable to perform detailed grading. Basic score based on essay length.");
        }
    }

    /**
     * Helper class for essay grading result
     */
    private static class EssayGradingResult {
        double bandScore;
        String feedback;

        EssayGradingResult(double bandScore, String feedback) {
            this.bandScore = bandScore;
            this.feedback = feedback;
        }
    }

    /**
     * Get user test history
     */
    public List<IELTSTestHistoryResponse> getUserHistory() throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(
                () -> new IdInvalidException("User not authenticated"));
        User user = userRepository.findByEmail(email);

        List<IELTSTestHistory> histories = historyRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return histories.stream()
                .map(this::convertToHistoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get test by ID with questions
     */
    public IELTSTestResponse getTestById(Long testId) throws IdInvalidException {
        IELTSTest test = testRepository.findById(testId)
                .orElseThrow(() -> new IdInvalidException("Test not found"));
        return convertToResponse(test, false);
    }

    // ========== Private Helper Methods ==========

    private String buildAIPrompt(GenerateIELTSTestRequest request) {
        // Special handling for Writing skill - generate essay prompt instead of MCQ
        if ("Writing".equalsIgnoreCase(request.getSkill())) {
            return buildWritingPrompt(request);
        }

        // Special handling for Listening skill - generate section-based questions
        if ("Listening".equalsIgnoreCase(request.getSkill())) {
            return buildListeningPrompt(request);
        }

        int numQuestions = request.getDifficulty().equals("Easy") ? 10
                : request.getDifficulty().equals("Medium") ? 15 : 20;

        // Map difficulty to IELTS band score range
        String bandRange = request.getDifficulty().equals("Easy") ? "Band 5.5-6.5"
                : request.getDifficulty().equals("Medium") ? "Band 6.5-7.5" : "Band 7.5-8.5";

        // Build prompt directly without PromptTemplate to avoid placeholder conflicts
        String prompt = String.format(
                """
                        Generate an IELTS %s test with %s level and %s difficulty (%s level questions).

                        DIFFICULTY GUIDE:
                        - Easy (Band 5.5-6.5): Basic vocabulary, straightforward questions, clear answers
                        - Medium (Band 6.5-7.5): More complex vocabulary, requires inference, some tricky options
                        - Hard (Band 7.5-8.5): Advanced vocabulary, nuanced answers, requires deep understanding

                        Create %d multiple choice questions. Each question should have 4 options (A, B, C, D) with only ONE correct answer.

                        For Reading skill: Include a reading passage and questions about it.
                        For Listening skill: Create a natural English conversation or monologue script in the "passage" field.
                        The passage should be approximately 150-200 words (around 900-1200 characters) that will be converted to audio.
                        Make it sound like natural spoken English.
                        IMPORTANT: You MUST use "Man:" and "Woman:" labels for conversations to indicate different speakers. This allows the system to use different voices.
                        Examples for Listening:
                        - A conversation between a man and a woman (e.g., "Man: Hello. Woman: Hi there.")
                        - A monologue (e.g., "Man: Welcome to the news.")
                        - A dialogue in a real-life situation.

                        For Speaking: Create grammar and vocabulary questions (no passage needed).

                        IMPORTANT: For the correct answer, provide a detailed explanation (2-3 sentences) explaining WHY it is correct.
                        For incorrect answers, provide brief explanation (1 sentence) explaining WHY they are wrong.

                        Return ONLY a valid JSON object in this exact format (no markdown, no extra text):
                        {
                          "title": "IELTS %s %s Test - %s",
                          "durationMinutes": 60,
                          "questions": [
                            {
                              "questionNumber": 1,
                              "questionText": "Question text here",
                              "questionType": "multiple_choice",
                              "passage": "For Listening: Natural spoken English script (150-200 words with speaker labels). For Reading: Reading passage. For others: null or empty.",
                              "answers": [
                                {"answerOption": "A", "answerText": "Option A text", "isCorrect": false, "explanation": "This is incorrect because..."},
                                {"answerOption": "B", "answerText": "Option B text", "isCorrect": true, "explanation": "This is correct because... The speaker mentioned that..."},
                                {"answerOption": "C", "answerText": "Option C text", "isCorrect": false, "explanation": "This is wrong because..."},
                                {"answerOption": "D", "answerText": "Option D text", "isCorrect": false, "explanation": "This was not mentioned."}
                              ]
                            }
                          ]
                        }
                        """,
                request.getSkill(),
                request.getLevel(),
                request.getDifficulty(),
                bandRange,
                numQuestions,
                request.getLevel(),
                request.getSkill(),
                request.getDifficulty());

        return prompt;
    }

    /**
     * Build AI prompt specifically for IELTS Listening based on real IELTS sections
     * Easy -> Section 1/2, Medium -> Section 3, Hard -> Section 4
     */
    private String buildListeningPrompt(GenerateIELTSTestRequest request) {
        String difficulty = request.getDifficulty();
        String level = request.getLevel();
        int numQuestions = difficulty.equals("Easy") ? 10 : difficulty.equals("Medium") ? 15 : 20;

        // Determine section based on difficulty
        String section;
        String context;
        String questionType;
        String questionFormat;
        String speakerGuidance;

        if ("Easy".equalsIgnoreCase(difficulty)) {
            boolean isSection1 = Math.random() < 0.5;
            if (isSection1) {
                section = "Section 1";
                context = "an everyday conversation between two people (e.g., booking a hotel, making a reservation, asking for directions)";
                questionType = "form_completion";
                questionFormat = """
                        For Form Completion questions, use this format:
                        - questionType: "form_completion"
                        - questionText: The field label (e.g., "Customer name:", "Phone number:", "Check-in date:")
                        - answers: Only ONE answer with isCorrect=true containing the correct answer from the audio
                        Example: {"answerOption": "answer", "answerText": "John Smith", "isCorrect": true, "explanation": "The man said his name was John Smith"}
                        """;
                speakerGuidance = "Use \"Man:\" and \"Woman:\" labels. Include specific details like names, phone numbers, dates, addresses. IMPORTANT: When mentioning names or spellings, write them out letter by letter with hyphens (e.g., \"My surname is Allen, that's A-L-L-E-N\", \"The postcode is CB2, that's C-B-2\"). This simulates how names are spelled in real IELTS listening tests.";
            } else {
                section = "Section 2";
                context = "a monologue in a social context (e.g., a tour guide explaining a place, an announcement about facilities)";
                questionType = "multiple_choice";
                questionFormat = """
                        For Multiple Choice questions, use this format:
                        - questionType: "multiple_choice"
                        - answers: EXACTLY 5 options labeled A, B, C, D, E
                        - Set isCorrect=true for correct options, isCorrect=false for wrong options
                        - Each correct answer should have an explanation

                        ⚠️ IMPORTANT DISTRIBUTION REQUIREMENT:
                        - 30-40%% of questions should have 1 correct answer (traditional single-choice)
                        - 60-70%% of questions should have 2-3 correct answers (multi-select)

                        You MUST CREATE VARIETY - do NOT make all questions single-choice or all multi-select!
                        For example, if you have 10 questions:
                        - 3-4 questions with 1 correct answer
                        - 6-7 questions with 2 or 3 correct answers
                        """;
                speakerGuidance = "Use \"Man:\" or \"Woman:\" for a single speaker giving information about places, facilities, or events.";
            }
        } else if ("Medium".equalsIgnoreCase(difficulty)) {
            section = "Section 3";
            context = "an academic discussion between 2-4 people (e.g., students discussing a project with a tutor)";

            // Randomly choose between matching, MCQ, or sentence completion
            double rand = Math.random();
            if (rand < 1.0) { // 0% matching (was 0.33)
                questionType = "matching";

                // Randomly choose from 6 diverse matching types
                String[] matchingTopics = {
                        "People – Opinions|What opinion does each person have about|Sarah, Tom, Lisa, Mark|A. It's too expensive, B. It saves time, C. It's more reliable, D. It needs improvement",
                        "People – Problems|What problem does each person mention|Student 1, Student 2, Student 3, Student 4|A. Lack of time, B. Limited resources, C. Technical difficulties, D. Communication issues",
                        "Places – Features|What feature is mentioned for each place|Library, Sports Center, Cafeteria, Student Union|A. Extended opening hours, B. Free WiFi, C. Quiet study areas, D. Group booking available",
                        "Courses – Characteristics|What is described about each course|Psychology 101, History 202, Science 303, Art 404|A. Requires fieldwork, B. Has online component, C. Includes group project, D. Offers internship",
                        "Days – Activities|What activity is planned for each day|Monday, Tuesday, Wednesday, Thursday|A. Team meeting, B. Lab session, C. Guest lecture, D. Field trip",
                        "Statements – Speakers|Who made each statement|Dr. Brown, Prof. Smith, Dr. Lee, Prof. Wilson|A. More funding is needed, B. Research shows progress, C. Students should participate, D. The deadline is flexible"
                };

                int topicIndex = (int) (Math.random() * matchingTopics.length);
                String[] topicParts = matchingTopics[topicIndex].split("\\|");
                String topicType = topicParts[0];
                String questionPrefix = topicParts[1];
                String itemsList = topicParts[2];
                String optionsList = topicParts[3];

                questionFormat = String.format(
                        """
                                For Matching questions - Topic: %s

                                CRITICAL STRUCTURE - Options and Items must be DIFFERENT:
                                - OPTIONS (A, B, C, D): Answers/Features/Characteristics
                                - ITEMS (1-4): People/Places/Objects to match

                                ITEMS for this question: %s
                                OPTIONS for this question: %s

                                Format requirements:
                                - questionType: "matching"
                                - questionText: "%s [topic]? Options: %s"

                                - answers: Create EXACTLY 4 answers with RANDOM order (NOT A,B,C,D in sequence!)
                                  ⚠️ IMPORTANT: Shuffle the answer options randomly. Example of GOOD order: C, A, D, B or B, D, A, C
                                  ❌ BAD: A, B, C, D (too predictable!)

                                  Each answer MUST have format:
                                  {"answerOption": "X", "answerText": "ItemName", "isCorrect": true, "explanation": "DETAILED explanation with at least 15 words explaining exactly why this item matches this option based on what was said in the audio"}

                                MANDATORY EXPLANATION FORMAT - Each explanation must:
                                - Be at least 15 words long
                                - Reference specific words/phrases from the audio
                                - Clearly explain WHY this item matches this option

                                Example answers (notice random order C, A, D, B):
                                {"answerOption": "C", "answerText": "Dr. Brown", "isCorrect": true, "explanation": "Dr. Brown specifically mentioned that the initial findings are encouraging and more financial support will be crucial for the project."}
                                {"answerOption": "A", "answerText": "Prof. Smith", "isCorrect": true, "explanation": "Prof. Smith agreed with Dr. Brown about additional resources and emphasized the need to involve more undergraduate students."}
                                {"answerOption": "D", "answerText": "Dr. Lee", "isCorrect": true, "explanation": "Dr. Lee stated that while student involvement is valuable, the team needs to be realistic about the project scope and deadline."}
                                {"answerOption": "B", "answerText": "Prof. Wilson", "isCorrect": true, "explanation": "Prof. Wilson expressed confidence in the current trajectory and suggested aiming to meet the existing deadline."}

                                CRITICAL RULES:
                                - answerText = EXACT item name from this list: %s
                                  ⚠️ USE THESE EXACT NAMES, DO NOT SUBSTITUTE OR MIX!
                                  ❌ WRONG: Using "Student 2" when the list has "Sarah, Tom, Lisa, Mark"
                                - answerOption = RANDOMIZED letter (A, B, C, or D) - NOT in order!
                                - EVERY answer MUST have detailed explanation (15+ words)
                                - Audio transcript must use the SAME EXACT names: %s
                                """,
                        topicType, itemsList, optionsList, questionPrefix, optionsList, itemsList, itemsList);
            } else if (rand < 0.0) {
                questionType = "multiple_choice";
                questionFormat = """
                        For Multiple Choice questions, use this format:
                        - questionType: "multiple_choice"
                        - answers: EXACTLY 5 options labeled A, B, C, D, E
                        - Set isCorrect=true for correct options, isCorrect=false for wrong options
                        - Each correct answer should have an explanation

                        ⚠️ IMPORTANT DISTRIBUTION REQUIREMENT:
                        - 30-40%% of questions should have 1 correct answer (traditional single-choice)
                        - 60-70%% of questions should have 2-3 correct answers (multi-select)

                        You MUST CREATE VARIETY - do NOT make all questions single-choice or all multi-select!
                        """;
            } else { // 100% sentence_completion for testing
                questionType = "sentence_completion";
                questionFormat = """
                        For Sentence Completion questions, use this format:
                        - questionType: "sentence_completion"
                        - questionText: A sentence with a blank to fill (e.g., "The main advantage of the method is its ________.")
                        - answers: Only ONE answer with isCorrect=true containing the word(s) to fill the blank
                        """;
            }
            speakerGuidance = "Use \"Student 1:\", \"Student 2:\", \"Tutor:\" labels. Include academic discussion about opinions, plans, and coursework.";
        } else {
            section = "Section 4";
            context = "an academic lecture or monologue (e.g., a university lecture on history, science, or environment)";

            // Randomly choose between sentence completion or flowchart
            boolean isFlowchart = Math.random() < 1.0;
            if (isFlowchart) {
                questionType = "flowchart";
                questionFormat = """
                        For Flow-chart Completion questions:
                        - questionType: "flowchart"
                        - questionText: Put the FULL FLOW DESCRIPTION here in format:
                          "Complete the flowchart below showing the process of [topic].
                          Step 1: [text with ________ blank] →
                          Step 2: [text with ________ blank] →
                          Step 3: [text with ________ blank] →
                          Step 4: [text with ________ blank] →
                          Step 5: [text with ________ blank] →
                          Step 6: [text with ________ blank]"

                        IMPORTANT: EVERY step MUST have exactly ONE blank (________) to fill!

                        Example:
                        "Complete the flowchart below showing the water treatment process.
                        Step 1: ________ water is collected from source →
                        Step 2: Initial ________ removes large debris →
                        Step 3: Chemical ________ is added →
                        Step 4: Water passes through ________ filters →
                        Step 5: ________ process kills bacteria →
                        Step 6: Clean water stored in ________"

                        - answers: Create 6 answers (one per step/blank):
                          {"answerOption": "1", "answerText": "Raw", "isCorrect": true, "explanation": "The lecturer mentioned raw water is collected from rivers."}
                          {"answerOption": "2", "answerText": "screening", "isCorrect": true, "explanation": "Screening removes debris first."}
                          {"answerOption": "3", "answerText": "coagulant", "isCorrect": true, "explanation": "Chemical coagulant helps particles stick."}
                          {"answerOption": "4", "answerText": "sand", "isCorrect": true, "explanation": "Sand filters remove remaining particles."}
                          {"answerOption": "5", "answerText": "Disinfection", "isCorrect": true, "explanation": "Disinfection kills harmful bacteria."}
                          {"answerOption": "6", "answerText": "tanks", "isCorrect": true, "explanation": "Clean water is stored in tanks."}

                        RULES:
                        - Total 5-6 steps in the process flow
                        - EVERY step MUST have exactly ONE blank (________)
                        - Each blank answer is 1-2 words only
                        - Do NOT reveal answers in step text
                        - Answers numbered 1-6 matching step order
                        - Each answer MUST have detailed explanation (10+ words)
                        """;
            } else {
                questionType = "sentence_completion";
                questionFormat = """
                        For Sentence Completion questions, use this format:
                        - questionType: "sentence_completion"
                        - questionText: A sentence with a blank to fill
                        - answers: Only ONE answer with isCorrect=true containing the word(s) to fill the blank
                        """;
            }
            speakerGuidance = "Use \"Lecturer:\" or \"Professor:\" label. Use formal, academic language with complex vocabulary.";
        }

        // For matching and flowchart questions, only 1 question is needed (they contain
        // multiple items inside)
        if ("matching".equals(questionType) || "flowchart".equals(questionType)) {
            numQuestions = 1;
        }

        return String.format(
                """
                        Generate an IELTS Listening %s test with %s level.

                        DIFFICULTY GUIDE (based on %s):
                        - Easy (Band 5.5-6.5): Basic everyday vocabulary, clear pronunciation, straightforward answers
                        - Medium (Band 6.5-7.5): Academic vocabulary, some inference required, moderately complex
                        - Hard (Band 7.5-8.5): Advanced academic vocabulary, nuanced answers, fast speech, complex ideas

                        CONTEXT: Create %s

                        AUDIO SCRIPT REQUIREMENTS:
                        - Length: EXACTLY 100-150 words MAXIMUM (will be converted to audio, DO NOT exceed this limit)
                        - %s
                        - Keep dialogue/monologue short and focused

                        QUESTION TYPE: %s

                        %s

                        Create %d questions based on the audio script.

                        Return ONLY a valid JSON object (no markdown):
                        {
                          "title": "IELTS Listening %s - %s - %s",
                          "durationMinutes": 30,
                          "questions": [
                            {
                              "questionNumber": 1,
                              "questionText": "Question text here",
                              "questionType": "%s",
                              "passage": "The full audio script with speaker labels",
                              "answers": [
                                {"answerOption": "A", "answerText": "Option text", "isCorrect": false, "explanation": "Why"},
                                {"answerOption": "B", "answerText": "Option text", "isCorrect": true, "explanation": "Why correct"}
                              ]
                            }
                          ]
                        }

                        IMPORTANT: All questions should be answerable from the audio. Use the same passage for all questions.
                        """,
                section,
                level,
                difficulty,
                context,
                speakerGuidance,
                questionType,
                questionFormat,
                numQuestions,
                section,
                level,
                difficulty,
                questionType);
    }

    /**
     * Build AI prompt specifically for IELTS Writing Task (randomly Task 1 or Task
     * 2)
     */
    private String buildWritingPrompt(GenerateIELTSTestRequest request) {
        // Randomly select Task 1 (chart description) or Task 2 (essay)
        boolean isTask1 = Math.random() < 0.5;

        if (isTask1) {
            return buildWritingTask1Prompt(request);
        } else {
            return buildWritingTask2Prompt(request);
        }
    }

    /**
     * Build AI prompt for IELTS Writing Task 1 (chart/graph description)
     */
    private String buildWritingTask1Prompt(GenerateIELTSTestRequest request) {
        return String.format(
                """
                        Generate an IELTS Writing Task 1 prompt with %s level and %s difficulty.

                        Task 1 requires describing data from a chart/graph. Generate a chart with realistic data.

                        Requirements:
                        - Minimum word count: 150 words
                        - Time: 20 minutes
                        - Chart should show clear trends or comparisons

                        Choose ONE chart type randomly: "line", "bar", or "pie"

                        Return ONLY a valid JSON object in this exact format (no markdown, no extra text):
                        {
                          "title": "IELTS Writing Task 1 - %s - %s",
                          "durationMinutes": 20,
                          "questions": [
                            {
                              "questionNumber": 1,
                              "questionText": "The chart/graph shows [describe what the data represents]. Summarize the information by selecting and reporting the main features, and make comparisons where relevant.",
                              "questionType": "essay",
                              "passage": "Instructions: Write a report of at least 150 words. You should:\\n- Describe the main trends or features\\n- Make comparisons between data points\\n- Use appropriate vocabulary for describing data\\n- Organize your answer logically",
                              "minWords": 150,
                              "chartData": {
                                "chartType": "line",
                                "title": "Chart title describing the data",
                                "xAxisLabel": "X-axis label (e.g., Year, Month, Category)",
                                "yAxisLabel": "Y-axis label (e.g., Percentage, Number, Amount)",
                                "labels": ["2018", "2019", "2020", "2021", "2022"],
                                "datasets": [
                                  {"label": "Category A", "data": [25, 30, 35, 40, 45], "color": "#4A90E2"},
                                  {"label": "Category B", "data": [40, 38, 35, 32, 28], "color": "#E24A4A"}
                                ]
                              },
                              "sampleAnswer": "Write a sample band 7-8 answer (150-180 words) that accurately describes the chart trends with proper data vocabulary.",
                              "answers": []
                            }
                          ]
                        }

                        CHART TYPES AND DATA RULES:
                        - For "line" chart: Show trends over time (5-7 data points), use 1-3 datasets
                        - For "bar" chart: Show comparisons (4-6 categories), use 1-2 datasets
                        - For "pie" chart: Show proportions that sum to 100, use single dataset with 4-6 items

                        IMPORTANT:
                        - Generate realistic, meaningful data (not random numbers)
                        - Data should tell a clear story (increasing, decreasing, fluctuating)
                        - Use appropriate colors: #4A90E2 (blue), #E24A4A (red), #4AE290 (green), #E2A64A (orange)
                        - Keep topic appropriate for %s level
                        """,
                request.getLevel(),
                request.getDifficulty(),
                request.getLevel(),
                request.getDifficulty(),
                request.getLevel());
    }

    /**
     * Build AI prompt for IELTS Writing Task 2 (essay)
     */
    private String buildWritingTask2Prompt(GenerateIELTSTestRequest request) {
        return String.format(
                """
                        Generate an IELTS Writing Task 2 essay prompt with %s level and %s difficulty.

                        This is for a learning app, so the requirements are slightly relaxed:
                        - Minimum word count: 150 words (instead of standard 250)
                        - Time: 30 minutes

                        Create an essay topic that is appropriate for %s level learners.
                        Topics should be about: education, technology, environment, society, health, work, or culture.

                        Return ONLY a valid JSON object in this exact format (no markdown, no extra text):
                        {
                          "title": "IELTS Writing Task 2 - %s - %s",
                          "durationMinutes": 30,
                          "questions": [
                            {
                              "questionNumber": 1,
                              "questionText": "The essay topic/question here. Make it clear and specific. For example: 'Some people believe that social media has a negative impact on young people. To what extent do you agree or disagree?'",
                              "questionType": "essay",
                              "passage": "Instructions: Write an essay of at least 150 words. You should:\\n- Clearly state your opinion\\n- Provide reasons and examples to support your view\\n- Use proper paragraph structure (introduction, body, conclusion)\\n- Use a range of vocabulary and grammar structures",
                              "minWords": 150,
                              "sampleAnswer": "Write a sample band 7-8 essay answer here (around 200-250 words) that demonstrates good structure, vocabulary, and argumentation. This will be used as a reference for AI grading.",
                              "answers": []
                            }
                          ]
                        }

                        IMPORTANT:
                        - The questionText should be an engaging, thought-provoking topic
                        - The sampleAnswer should be a well-written example essay
                        - Keep the topic appropriate for %s level
                        """,
                request.getLevel(),
                request.getDifficulty(),
                request.getLevel(),
                request.getLevel(),
                request.getDifficulty(),
                request.getLevel());
    }

    private String generateWithAI(String prompt) {
        try {
            // Using AiClientService
            String response = aiClientService.generate(prompt);

            log.info("AI Response: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Error generating test with AI", e);
            throw new RuntimeException("Failed to generate test: " + e.getMessage());
        }
    }

    private IELTSTest parseAndSaveTest(String aiResponse, GenerateIELTSTestRequest request)
            throws JsonProcessingException {
        // Clean up response (remove markdown if present)
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

        IELTSTest test = new IELTSTest();
        test.setSkill(request.getSkill());
        test.setLevel(request.getLevel());
        test.setDifficulty(request.getDifficulty());
        test.setTitle(rootNode.get("title").asText());
        test.setDurationMinutes(rootNode.get("durationMinutes").asInt());
        test.setTotalQuestions(rootNode.get("questions").size());

        // Parse questions
        JsonNode questionsNode = rootNode.get("questions");
        for (JsonNode questionNode : questionsNode) {
            IELTSQuestion question = new IELTSQuestion();
            question.setTest(test);
            question.setQuestionNumber(questionNode.get("questionNumber").asInt());
            question.setQuestionText(questionNode.get("questionText").asText());
            question.setQuestionType(
                    questionNode.has("questionType") ? questionNode.get("questionType").asText() : "multiple_choice");

            // Handle passage and audio for Listening tests
            String passage = null;
            if (questionNode.has("passage") && !questionNode.get("passage").isNull()) {
                passage = questionNode.get("passage").asText();
                question.setPassage(passage);
            }

            // Handle Writing-specific fields
            if ("Writing".equalsIgnoreCase(request.getSkill())) {
                if (questionNode.has("sampleAnswer") && !questionNode.get("sampleAnswer").isNull()) {
                    question.setSampleAnswer(questionNode.get("sampleAnswer").asText());
                }
                if (questionNode.has("minWords") && !questionNode.get("minWords").isNull()) {
                    question.setMinWords(questionNode.get("minWords").asInt());
                } else {
                    question.setMinWords(150); // Default 150 words for learning app
                }
                // Handle Task 1 chart data
                if (questionNode.has("chartData") && !questionNode.get("chartData").isNull()) {
                    question.setChartData(questionNode.get("chartData").toString());
                }
            }

            // Generate audio URL for Listening tests
            if ("Listening".equalsIgnoreCase(test.getSkill())) {
                // Use passage if available, otherwise use question text
                String textToConvert = (passage != null && !passage.isEmpty())
                        ? passage
                        : question.getQuestionText();

                String audioUrl = textToSpeechService.generateWithResponsiveVoice(textToConvert);
                question.setAudioUrl(audioUrl);
                log.info("Generated audio URL for Listening question {}: {}", question.getQuestionNumber(), audioUrl);
            }

            // Parse answers (only for non-essay questions)
            JsonNode answersNode = questionNode.get("answers");
            if (answersNode != null && answersNode.isArray()) {
                for (JsonNode answerNode : answersNode) {
                    // Skip if answerNode doesn't have required fields (essay questions have empty
                    // answers array)
                    if (!answerNode.has("answerOption") || answerNode.get("answerOption").isNull()) {
                        continue;
                    }
                    IELTSAnswer answer = new IELTSAnswer();
                    answer.setQuestion(question);
                    answer.setAnswerOption(answerNode.get("answerOption").asText());
                    answer.setAnswerText(answerNode.get("answerText").asText());
                    answer.setIsCorrect(answerNode.get("isCorrect").asBoolean());
                    if (answerNode.has("explanation") && !answerNode.get("explanation").isNull()) {
                        answer.setExplanation(answerNode.get("explanation").asText());
                    }
                    question.getAnswers().add(answer);
                }

                // Log MCQ correct answer count for debugging
                if ("multiple_choice".equalsIgnoreCase(question.getQuestionType())) {
                    long correctCount = question.getAnswers().stream()
                            .filter(IELTSAnswer::getIsCorrect)
                            .count();
                    log.info("MCQ question {} has {} correct answer(s)",
                            question.getQuestionNumber(), correctCount);
                }
            }

            test.getQuestions().add(question);
        }

        // Post-processing: Ensure 60-70% of MCQ questions have multiple correct answers
        java.util.List<IELTSQuestion> mcqQuestions = test.getQuestions().stream()
                .filter(q -> "multiple_choice".equalsIgnoreCase(q.getQuestionType()))
                .collect(Collectors.toList());

        if (!mcqQuestions.isEmpty()) {
            long singleChoiceCount = mcqQuestions.stream()
                    .filter(q -> q.getAnswers().stream().filter(IELTSAnswer::getIsCorrect).count() == 1)
                    .count();

            double singleChoiceRatio = (double) singleChoiceCount / mcqQuestions.size();
            log.info("MCQ distribution: {} single-choice out of {} total ({}%)",
                    singleChoiceCount, mcqQuestions.size(), Math.round(singleChoiceRatio * 100));

            // If more than 40% are single-choice, convert some to multi-select
            if (singleChoiceRatio > 0.4) {
                int needToConvert = (int) (singleChoiceCount - (mcqQuestions.size() * 0.35)); // Target 35% single
                log.info("Converting {} single-choice questions to multi-select", needToConvert);

                java.util.List<IELTSQuestion> singleChoiceQuestions = mcqQuestions.stream()
                        .filter(q -> q.getAnswers().stream().filter(IELTSAnswer::getIsCorrect).count() == 1)
                        .collect(Collectors.toList());
                java.util.Collections.shuffle(singleChoiceQuestions);

                for (int i = 0; i < needToConvert && i < singleChoiceQuestions.size(); i++) {
                    IELTSQuestion q = singleChoiceQuestions.get(i);
                    // Find incorrect answers and make 1-2 more correct
                    java.util.List<IELTSAnswer> incorrectAnswers = q.getAnswers().stream()
                            .filter(a -> !a.getIsCorrect())
                            .collect(Collectors.toList());
                    java.util.Collections.shuffle(incorrectAnswers);

                    int toMakeCorrect = 1 + (int) (Math.random() * 2); // 1 or 2 more
                    for (int j = 0; j < toMakeCorrect && j < incorrectAnswers.size(); j++) {
                        IELTSAnswer ans = incorrectAnswers.get(j);
                        ans.setIsCorrect(true);
                        if (ans.getExplanation() == null || ans.getExplanation().isEmpty()) {
                            ans.setExplanation(
                                    "This option is also a correct answer based on the information provided.");
                        }
                    }
                    log.info("Converted MCQ question {} to multi-select with {} correct answers",
                            q.getQuestionNumber(), q.getAnswers().stream().filter(IELTSAnswer::getIsCorrect).count());
                }
            }
        }

        return testRepository.save(test);
    }

    private BigDecimal calculateIELTSBandScore(double percentage) {
        // Simplified IELTS band score calculation
        double score;
        if (percentage >= 0.90)
            score = 9.0;
        else if (percentage >= 0.82)
            score = 8.5;
        else if (percentage >= 0.75)
            score = 8.0;
        else if (percentage >= 0.67)
            score = 7.5;
        else if (percentage >= 0.60)
            score = 7.0;
        else if (percentage >= 0.52)
            score = 6.5;
        else if (percentage >= 0.45)
            score = 6.0;
        else if (percentage >= 0.37)
            score = 5.5;
        else if (percentage >= 0.30)
            score = 5.0;
        else if (percentage >= 0.22)
            score = 4.5;
        else
            score = 4.0;

        return BigDecimal.valueOf(score).setScale(1, RoundingMode.HALF_UP);
    }

    private IELTSTestResponse convertToResponse(IELTSTest test, boolean includeCorrectAnswers) {
        IELTSTestResponse response = new IELTSTestResponse();
        response.setId(test.getId());
        response.setSkill(test.getSkill());
        response.setLevel(test.getLevel());
        response.setDifficulty(test.getDifficulty());
        response.setTitle(test.getTitle());
        response.setDurationMinutes(test.getDurationMinutes());
        response.setTotalQuestions(test.getTotalQuestions());

        List<IELTSQuestionResponse> questions = test.getQuestions().stream()
                .map(q -> {
                    IELTSQuestionResponse qr = new IELTSQuestionResponse();
                    qr.setId(q.getId());
                    qr.setQuestionNumber(q.getQuestionNumber());
                    qr.setQuestionText(q.getQuestionText());
                    qr.setQuestionType(q.getQuestionType());
                    qr.setPassage(q.getPassage());
                    qr.setAudioUrl(q.getAudioUrl());
                    qr.setSampleAnswer(q.getSampleAnswer());
                    qr.setMinWords(q.getMinWords());
                    qr.setChartData(q.getChartData());

                    List<IELTSAnswerResponse> answers = q.getAnswers().stream()
                            .map(a -> {
                                IELTSAnswerResponse ar = new IELTSAnswerResponse();
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

    private IELTSTestHistoryResponse convertToHistoryResponse(IELTSTestHistory history) {
        IELTSTestHistoryResponse response = new IELTSTestHistoryResponse();
        response.setId(history.getId());
        response.setTestId(history.getTest().getId());
        response.setTestTitle(history.getTest().getTitle());
        response.setSkill(history.getTest().getSkill());
        response.setLevel(history.getTest().getLevel());
        response.setDifficulty(history.getTest().getDifficulty());
        response.setStartedAt(history.getStartedAt());
        response.setCompletedAt(history.getCompletedAt());
        response.setScore(history.getScore() != null ? history.getScore().doubleValue() : null);
        response.setCorrectAnswers(history.getCorrectAnswers());
        response.setTotalAnswers(history.getTotalAnswers());
        response.setStatus(history.getStatus());
        response.setTimeSpentSeconds(history.getTimeSpentSeconds());
        return response;
    }
}
