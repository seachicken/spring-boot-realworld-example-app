package io.spring.api;

import io.spring.api.exception.NoAuthorizationException;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ArticleQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.article.UpdateArticleParam;
import io.spring.application.data.ArticleData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.service.AuthorizationService;
import io.spring.core.user.User;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@ApplicationEntrypoint
@RequestMapping(path = "/articles/{slug}")
@AllArgsConstructor
public class ArticleApi {
  private ArticleQueryService articleQueryService;
  private ArticleRepository articleRepository;
  private ArticleCommandService articleCommandService;

  @GetMapping
  public ResponseEntity<?> article(
      @PathVariable("slug") String slug, @AuthenticationPrincipal User user) {
    return articleQueryService
        .findBySlug(slug, user)
        .map(articleData -> ResponseEntity.ok(articleResponse(articleData)))
        .orElseThrow(ResourceNotFoundException::new);
  }

  @PutMapping
  public ResponseEntity<?> updateArticle(
      @PathVariable("slug") String slug,
      @AuthenticationPrincipal User user,
      @Valid @RequestBody UpdateArticleParam updateArticleParam) {
    return articleRepository
        .findBySlug(slug)
        .map(
            article -> {
              if (!AuthorizationService.canWriteArticle(user, article)) {
                throw new NoAuthorizationException();
              }
              Article updatedArticle =
                  articleCommandService.updateArticle(article, updateArticleParam);
              return ResponseEntity.ok(
                  articleResponse(
                      articleQueryService.findBySlug(updatedArticle.getSlug(), user).get()));
            })
        .orElseThrow(ResourceNotFoundException::new);
  }

  @DeleteMapping
  public ResponseEntity deleteArticle(
      @PathVariable("slug") String slug, @AuthenticationPrincipal User user) {
    return articleRepository
        .findBySlug(slug)
        .map(
            article -> {
              if (!AuthorizationService.canWriteArticle(user, article)) {
                throw new NoAuthorizationException();
              }
              articleRepository.remove(article);
              return ResponseEntity.noContent().build();
            })
        .orElseThrow(ResourceNotFoundException::new);
  }

  private Map<String, Object> articleResponse(ArticleData articleData) {
    return new HashMap<String, Object>() {
      {
        put("article", articleData);
      }
    };
  }
}
