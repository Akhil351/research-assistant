package akhil.research.assistant.service.impl;

import akhil.research.assistant.request.ResearchRequest;
import akhil.research.assistant.response.GeminiResponse;
import akhil.research.assistant.service.ResearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResearchServiceIml implements ResearchService {
    @Value("${gemini.api.url}")
    private String geminiApiUrl;
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Autowired
    private WebClient webClient;
    @Autowired
    private ObjectMapper objectMapper;
    @Override
    public String processContent(ResearchRequest request) {
        String prompt=buildPrompt(request);
        Map<String,Object>requestBody=new HashMap<>();
        requestBody.put("contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))
        ));
        String response = webClient.post()
                .uri(geminiApiUrl + geminiApiKey)  // Set API URL
                .bodyValue(requestBody)            // Set request body
                .retrieve()                         // Send request & get response
                .bodyToMono(String.class)           // Convert response body to Mono<String>
                .block();                           // Block (convert Mono to String)
        return extractTextFromResponse(response);
    }

    private String extractTextFromResponse(String response) {
        try{
            GeminiResponse geminiResponse=objectMapper.readValue(response, GeminiResponse.class);
            if(geminiResponse.getCandidates()!=null && !geminiResponse.getCandidates().isEmpty()){
                  GeminiResponse.Candidate firstCandidate=geminiResponse.getCandidates().getFirst();
                  if(firstCandidate.getContent()!=null && firstCandidate.getContent().getParts()!=null
                          && !firstCandidate.getContent().getParts().isEmpty()){
                      return firstCandidate.getContent().getParts().getFirst().getText();
                  }
            }
            return "No Content found in response";
        }
        catch (Exception e){
            return "Error Parsing :"+e.getMessage();
        }
    }
//    {
//        "contents": [{
//        "parts":[{"text": "Explain how AI works"}]
//    }]
//    }

    private String buildPrompt(ResearchRequest request){
        StringBuilder prompt=new StringBuilder();
        switch(request.getOperation()){
            case "summarize":
                prompt.append("Provide a clear and concise summary of the following text in a few sentences:\n\n");
                break;
            case "suggest":
                prompt.append("Based on the following content: suggest related topics and further reading . Format the response with clear headings and bullet points:\n\n");
                break;
            default:
                throw new IllegalArgumentException("UnKnow Operation: "+request.getOperation());
        }
        prompt.append(request.getContent());
        return prompt.toString();
    }
}


//{
//        "candidates": [
//        {
//        "content": {
//        "parts": [
//        {
//        "text": "I don't have a name.  I am a large language model.\n"
//        }
//        ],
//        "role": "model"
//        },
//        "finishReason": "STOP",
//        "avgLogprobs": -0.07305300235748291
//        }
//        ],
//        "usageMetadata": {
//        "promptTokenCount": 4,
//        "candidatesTokenCount": 17,
//        "totalTokenCount": 21,
//        "promptTokensDetails": [
//        {
//        "modality": "TEXT",
//        "tokenCount": 4
//        }
//        ],
//        "candidatesTokensDetails": [
//        {
//        "modality": "TEXT",
//        "tokenCount": 17
//        }
//        ]
//        },
//        "modelVersion": "gemini-1.5-flash"
//        }