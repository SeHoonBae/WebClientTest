package com.webclient.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.LoggingCodecSupport;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class WebClientConfig {

    @Bean
    public WebClient webClient(){
        /**
         * Spring WebFlux 에서는 어플리케이션 메모리 문제를 피하기 위해 codec 처리를 위한 in-memory buffer 값이 256KB로 기본 설정 되어 있습니다.
         * 이 제약 때문에 256KB보다 큰 http 메시지를 처리하려고 하면 DataBufferLimitException 에러가 발생하게 됩니다.
         * 이 값을 늘려주기 위해서는 ExchangeStrategies.builder()를 통해 값을 늘려줘야 합니다.
         */
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024*1024*50))
                .build();

        /**
         * Debug 레벨일 때 form Data와 Trace 레벨 일 때 header 정보는 민감한 정보를 포함하고 있기 때문에
         * 기본 WebClient 설정에서는 위 정보를 로그에서 확인할 수 없습니다.
         * 개발 진행 시 Request / Response 정보를 상세히 확인하기 위해서는 ExchangeStrateges 와 logging level 설정을 통해
         * 로그 확인이 가능하도록 해주는 것이 좋습니다.
         *
         * ExchangeStrategies 를 통해 setEnableLoggingRequestDetails(boolean enable)을 true로 설정해 주고
         * application.yml에 개발용 로깅 레벨은 debug로 설정해 줍니다.
         *
         * logging:
         *  level:
         *      org.springframework.web.reactive.function.client.ExchangeFunctions: debug
         */
        exchangeStrategies
                .messageWriters().stream()
                .filter(LoggingCodecSupport.class::isInstance)
                .forEach(writer -> ((LoggingCodecSupport)writer).setEnableLoggingRequestDetails(true));

        /**
         *
         * HttpClient TimeOut
         * HttpClient를 변경하거나 ConnectionTimeOut과 같은 설정값을 변경하려면
         * WebClient.builder().clientConnector()를 통해 Reactor Netty의 HttpClient를 직접 설정해줘야 합니다.
         * 해당 코드의 line 76 ~ 81
         *
         * Client Filters
         * Request 또는 Response 데이터에 대해 조작을 하거나 추가 작업을 하기 위해서는
         * WebClient.builder().filter() 메소드를 이용해야 합니다.
         * ExchangeFilterFunction.ofRequestProcessor() 와
         * ExchangeFilterFunction.ofResponseProcessor()를 통해 clientRequest와
         * clientResponse를 변경하거나 출력할 수 있습니다.
         * 해당 코드의 line 88 ~ 105
         *
         */
        return WebClient.builder()
                .clientConnector(
                        new ReactorClientHttpConnector(
                                HttpClient.create()
                                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                                        .responseTimeout(Duration.ofMillis(5000))
                                        .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS))
                                                .addHandlerLast(new WriteTimeoutHandler(5000, TimeUnit.MILLISECONDS))
                                        )
                                        .secure(
                                                sslContextSpec -> {
                                                    try {
                                                        sslContextSpec.sslContext(
                                                                SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
                                                        );
                                                    } catch (SSLException e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                }
                                        )
                        )
                )
                .exchangeStrategies(exchangeStrategies)
                .filter(ExchangeFilterFunction.ofRequestProcessor(
                        clientRequest -> {
                            log.debug("Request: {} {} ", clientRequest.method(), clientRequest.url());
                            clientRequest.headers().forEach(
                                    (name, values) -> values.forEach(
                                            value -> log.debug("{} : {}", name, value))
                            );
                            return Mono.just(clientRequest);
                        }
                ))
                .filter(ExchangeFilterFunction.ofResponseProcessor(
                        clientResponse -> {
                            clientResponse.headers().asHttpHeaders().forEach(
                                    (name, values) -> values.forEach(value -> log.debug("{} : {}", name, value))
                            );
                            return Mono.just(clientResponse);
                        }
                ))
                .build();
    }
}
