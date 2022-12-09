package com.webclient.service;

import com.webclient.dto.Info;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebClientService {

    private final WebClient webClient;

    public Mono<String> getWebClientTest(String name, String id) {

        log.info("getWebClientTest start!!");
        Mono<String> result = webClient.mutate()
                .baseUrl("http://localhost:8080")
                .build()
                .get()
                .uri(uri -> uri.path("/mockmvc")
                        .queryParam("name", name)
                        .queryParam("id", id)
                        .build()
                )
                .retrieve()
                .bodyToMono(String.class);

        /**
         * WebClient는 결과를 Mono: 0~1개, Flux: 0~N개 형태로 최종 전달하지 않는 이상
         * 코드 내에서 Subscribe()를 실행해야 실제 http 호출이 진행된다.
         *
         * 결과: 해당 메소드가 다 호출 된 후 해당 api의 결과값을 전달 - non-blocking이기 때문에 나오는 현상
         * 2022-12-09T18:46:37.132+09:00  INFO 1980 --- [ctor-http-nio-3] com.webclient.service.WebClientService   : getWebClientTest start!!
         * 2022-12-09T18:46:37.313+09:00  INFO 1980 --- [ctor-http-nio-3] com.webclient.service.WebClientService   : getWebClientTest end!!
         * 2022-12-09T18:46:37.387+09:00  INFO 1980 --- [ctor-http-nio-3] com.webclient.service.WebClientService   : 테스트의 MockMvc 테스트입니다. test
         */
        result.subscribe(e -> log.info(e));
        log.info("getWebClientTest end!!");

        return result;
    }

    public void postWebClientTest(String name, String id) {
        log.info("postWebClientTest start!!");

        Info info = new Info(name, id);

        Mono<String> result = webClient.mutate()
                .baseUrl("http://localhost:8080")
                .build()
                .post()
                .uri("/mockmvc")
//                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(info)
                .retrieve()
                .bodyToMono(String.class);

        result.subscribe(e -> log.info(e));

        log.info("postWebClientTest end!!");
    }
}
