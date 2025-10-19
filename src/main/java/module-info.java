module dev.cerios.maugame.mauengine {
    requires static lombok;
    requires org.apache.commons.collections4;
    requires org.slf4j;
    requires com.github.f4b6a3.ulid;

    exports dev.cerios.maugame.mauengine.game;
    exports dev.cerios.maugame.mauengine.card;
    exports dev.cerios.maugame.mauengine.exception;
}