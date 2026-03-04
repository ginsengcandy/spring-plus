package org.example.expert;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultHandler;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;

@AutoConfigureRestDocs
public abstract class RestDocsSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // 모든 테스트에서 공통으로 적용할 문서 포맷 설정
    protected final ResultHandler restDocsHandler(String identifier) {
        return document(identifier,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())
        );
    }
}