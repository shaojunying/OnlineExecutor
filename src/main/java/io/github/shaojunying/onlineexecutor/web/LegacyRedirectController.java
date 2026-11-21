package io.github.shaojunying.onlineexecutor.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Keeps the old page URL working. The page now lives at {@code /}.
 */
@Controller
class LegacyRedirectController {

    @GetMapping("/executor")
    RedirectView executorPage() {
        RedirectView view = new RedirectView("/");
        view.setStatusCode(org.springframework.http.HttpStatus.MOVED_PERMANENTLY);
        return view;
    }
}
