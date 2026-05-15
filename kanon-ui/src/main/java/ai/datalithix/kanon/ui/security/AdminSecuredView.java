package ai.datalithix.kanon.ui.security;

import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

public interface AdminSecuredView extends BeforeEnterObserver {
    CurrentUserContext currentUserContext();

    @Override
    default void beforeEnter(BeforeEnterEvent event) {
        if (!AdminRouteAccess.canEnter(getClass(), currentUserContext())) {
            event.rerouteToError(AccessDeniedException.class, "Access denied");
        }
    }
}
