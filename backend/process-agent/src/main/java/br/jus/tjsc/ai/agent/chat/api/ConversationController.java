package br.jus.tjsc.ai.agent.chat.api;

import br.jus.tjsc.ai.agent.chat.api.dto.ConversationDetail;
import br.jus.tjsc.ai.agent.chat.api.dto.ConversationPage;
import br.jus.tjsc.ai.agent.chat.application.ConversationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/conversations")
class ConversationController {

    private final ConversationService conversationService;

    ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    ConversationPage list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return conversationService.listConversations(page, size);
    }

    @GetMapping("/{id}")
    ConversationDetail get(@PathVariable String id) {
        return conversationService.getConversation(id);
    }

    @PatchMapping("/{id}/title")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateTitle(@PathVariable String id, @RequestBody TitleRequest request) {
        conversationService.updateTitle(id, request.title());
    }

    @PatchMapping("/{id}/starred")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void setStarred(@PathVariable String id, @RequestBody StarredRequest req) {
        conversationService.setStarred(id, req.starred());
    }

    @PatchMapping("/{id}/archived")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void setArchived(@PathVariable String id, @RequestBody ArchivedRequest req) {
        conversationService.setArchived(id, req.archived());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable String id) {
        conversationService.deleteConversation(id);
    }

    record TitleRequest(String title) {}
    record StarredRequest(boolean starred) {}
    record ArchivedRequest(boolean archived) {}
}
