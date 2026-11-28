package org.unitedlands.unitedlands.commands.handlers.country.lists;

import java.util.Collection;
import java.util.List;

import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.commandhandlers.PaginatedListCommandHandler;
import org.unitedlands.unitedlands.classes.lists.ListCriterion;
import org.unitedlands.unitedlands.commands.handlers.country.CmdCountry;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdCountry.class,
        name = "list",
        description = "Lists all countries",
        usage = "/country list [criterion] [asc|desc] [page]"
)
public class CmdCountryList extends PaginatedListCommandHandler<Country> {

    private static final List<ListCriterion<Country>> CRITERIA = List.of(
            new ListCriterion<>("population", Country::getCitizenCount,
                    (c, v) -> United.messenger().get("player.lists.values.citizens", v), true),
            new ListCriterion<>("regions", Country::getRegionCount,
                    (c, v) -> United.messenger().get("player.lists.values.regions", v), true),
            new ListCriterion<>("settlements", Country::getSettlementCount,
                    (c, v) -> United.messenger().get("player.lists.values.settlements", v), true),
            new ListCriterion<>("balance", Country::getBalance,
                    (c, v) -> United.messenger().get("player.lists.values.balance",
                            UnitedLandsEconomyManager.instance().format((double) v)), true),
            new ListCriterion<>("founded", Country::getFoundingTimestamp,
                    (c, v) -> United.messenger().get("player.lists.values.founded", formatDate(v)), false),
            new ListCriterion<>("name", c -> c.getCleanName().toLowerCase(),
                    (c, v) -> United.messenger().get("player.lists.values.citizens", c.getCitizenCount()), false));

    @Override
    protected Collection<Country> getEntries() {
        return UnitedLandsDataManager.instance().getCountries();
    }

    @Override
    protected List<ListCriterion<Country>> getCriteria() {
        return CRITERIA;
    }

    @Override
    protected String getEntryName(Country entry) {
        return entry.getCleanName();
    }

    @Override
    protected String getInfoCommand(Country entry) {
        return "/country info " + entry.getName();
    }

    @Override
    protected String getListCommand() {
        return "/country list";
    }

    @Override
    protected String getMessagePath() {
        return "player.lists.countries";
    }

}
