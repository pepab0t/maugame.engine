package dev.cerios.maugame.mauengine.game.action;

public class DisqualifiedAction implements Action {
    @Override
    public ActionType getType() {
        return ActionType.DISQUALIFIED;
    }
}
