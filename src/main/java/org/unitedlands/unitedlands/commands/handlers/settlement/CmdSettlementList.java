package org.unitedlands.unitedlands.commands.handlers.settlement;

import java.util.Collection;
import java.util.List;

import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.commandhandlers.PaginatedListCommandHandler;
import org.unitedlands.unitedlands.classes.lists.ListCriterion;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdSettlement.class,
        name = "list",
        description = "Lists all settlements",
        usage = "/settlement list [criterion] [asc|desc] [page]"
)
public class CmdSettlementList extends PaginatedListCommandHandler<Settlement> {

    private static final List<ListCriterion<Settlement>> CRITERIA = List.of(
            new ListCriterion<>("citizens", Settlement::getCitizenCount,
                    (s, v) -> United.messenger().get("player.lists.values.citizens", v), true),
            new ListCriterion<>("size", Settlement::getSize,
                    (s, v) -> United.messenger().get("player.lists.values.chunks", v), true),
            new ListCriterion<>("balance", Settlement::getBalance,
                    (s, v) -> United.messenger().get("player.lists.values.balance",
                            UnitedLandsEconomyManager.instance().format((double) v)), true),
            new ListCriterion<>("upkeep", Settlement::getUpkeep,
                    (s, v) -> United.messenger().get("player.lists.values.upkeep",
                            UnitedLandsEconomyManager.instance().format((double) v)), true),
            new ListCriterion<>("founded", Settlement::getFoundingTimestamp,
                    (s, v) -> United.messenger().get("player.lists.values.founded", formatDate(v)), false),
            new ListCriterion<>("name", s -> s.getCleanName().toLowerCase(),
                    (s, v) -> United.messenger().get("player.lists.values.citizens", s.getCitizenCount()), false));

    @Override
    protected Collection<Settlement> getEntries() {
        return UnitedLandsDataManager.instance().getSettlements();
    }

    @Override
    protected List<ListCriterion<Settlement>> getCriteria() {
        return CRITERIA;
    }

    @Override
    protected String getEntryName(Settlement entry) {
        return entry.getCleanName();
    }

    @Override
    protected String getInfoCommand(Settlement entry) {
        return "/settlement info " + entry.getName();
    }

    @Override
    protected String getListCommand() {
        return "/settlement list";
    }

    @Override
    protected String getMessagePath() {
        return "player.lists.settlements";
    }

}
