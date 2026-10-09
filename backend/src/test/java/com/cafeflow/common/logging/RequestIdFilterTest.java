package com.cafeflow.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void generatesAnIdWhenNoneIsSupplied() throws Exception {
        MockHttpServletResponse response = run(new MockHttpServletRequest(), new AtomicReference<>());

        assertThat(response.getHeader(RequestIdFilter.HEADER)).isNotBlank();
    }

    @Test
    void echoesAWellFormedClientId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "abc12345-def");
        AtomicReference<String> seenInMdc = new AtomicReference<>();

        MockHttpServletResponse response = run(request, seenInMdc);

        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("abc12345-def");
        assertThat(seenInMdc.get()).isEqualTo("abc12345-def");
    }

    @Test
    void replacesAMaliciousClientIdToPreventLogInjection() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "x\n{\"level\":\"FATAL\"}");

        MockHttpServletResponse response = run(request, new AtomicReference<>());

        assertThat(response.getHeader(RequestIdFilter.HEADER)).doesNotContain("\n", "{");
    }

    @Test
    void clearsTheMdcAfterTheRequest() throws Exception {
        run(new MockHttpServletRequest(), new AtomicReference<>());

        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    private MockHttpServletResponse run(
            MockHttpServletRequest request, AtomicReference<String> mdcDuringRequest)
            throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(
                request,
                response,
                (req, res) -> {
                    mdcDuringRequest.set(MDC.get(RequestIdFilter.MDC_KEY));
                    chain.doFilter(req, res);
                });
        return response;
    }
}
