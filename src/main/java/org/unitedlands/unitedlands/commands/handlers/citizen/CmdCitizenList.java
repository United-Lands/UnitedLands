package org.unitedlands.unitedlands.commands.handlers.citizen;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.commandhandlers.PaginatedListCommandHandler;
import org.unitedlands.unitedlands.classes.lists.ListCriterion;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdCitizen.class,
        name = "list",
        description = "Lists all citizens",
        usage = "/citizen list [criterion] [asc|desc] [page]"
)
public class CmdCitizenList extends PaginatedListCommandHandler<Citizen> {

    private static final List<ListCriterion<Citizen>> CRITERIA = List.of(
            new ListCriterion<>("balance", c -> UnitedLandsEconomyManager.instance().getBalance(c.getUuid()),
                    (c, v) -> United.messenger().get("player.lists.values.balance",
                            UnitedLandsEconomyManager.instance().format((BigDecimal) v)), true),
            new ListCriterion<>("joined", Citizen::getJoined,
                    (c, v) -> United.messenger().get("player.lists.values.joined", formatDate(v)), false),
            new ListCriterion<>("name", c -> nameOf(c).toLowerCase(),
                    (c, v) -> United.messenger().get("player.lists.values.settlement",
                            c.getSettlement() != null ? c.getSettlement().getCleanName() : "-"), false));

    @Override
    protected Collection<Citizen> getEntries() {
        return UnitedLandsDataManager.instance().getCitizens();
    }

    @Override
    protected List<ListCriterion<Citizen>> getCriteria() {
        return CRITERIA;
    }

    @Override
    protected String getEntryName(Citizen entry) {
        return nameOf(entry);
    }

    @Override
    protected String getInfoCommand(Citizen entry) {
        return "/citizen info " + nameOf(entry);
    }

    @Override
    protected String getListCommand() {
        return "/citizen list";
    }

    @Override
    protected String getMessagePath() {
        return "player.lists.citizens";
    }

    private static String nameOf(Citizen citizen) {
        return citizen.getName() != null ? citizen.getName() : citizen.getUuid().toString();
    }

}
