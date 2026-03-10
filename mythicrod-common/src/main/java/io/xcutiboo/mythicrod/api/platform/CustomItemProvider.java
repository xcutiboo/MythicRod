package io.xcutiboo.mythicrod.api.platform;

import java.util.Optional;

public interface CustomItemProvider {
    Optional<PlatformItem> buildItem(String id, ItemContext context);
}
