package com.loopers.application.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LikeConcurrencyTest {

    @Autowired
    private LikeFacade likeFacade;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String RAW_PASSWORD = "Test1234!";

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("10명의 사용자가 동시에 같은 상품에 좋아요를 누르면, likesCount가 10이 된다.")
    @Test
    void likesCountBecomeTen_whenTenUsersLikeSameProductConcurrently() throws InterruptedException {
        // Arrange
        Brand brand = brandJpaRepository.save(new Brand("테스트브랜드", "설명"));
        Product product = productJpaRepository.save(
            new Product(brand.getId(), "테스트상품", 10000, 100, "설명", null)
        );

        int threadCount = 10;
        String encodedPassword = passwordEncoder.encode(RAW_PASSWORD);
        List<String> loginIds = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            String loginId = "likeuser" + i;
            userJpaRepository.save(
                new User(loginId, encodedPassword, "사용자" + i, "19900101", "user" + i + "@test.com")
            );
            loginIds.add(loginId);
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // Act
        for (int i = 0; i < threadCount; i++) {
            final String loginId = loginIds.get(i);
            executor.submit(() -> {
                latch.countDown();
                try {
                    latch.await();
                    likeFacade.addLike(loginId, RAW_PASSWORD, product.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // Assert
        Product found = productJpaRepository.findById(product.getId()).orElseThrow();
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(found.getLikesCount()).isEqualTo(10);
    }
}
