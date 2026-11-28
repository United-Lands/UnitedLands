package org.unitedlands.unitedlands.classes.commandhandlers;

import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.classes.lists.ListCriterion;
import org.unitedlands.utils.United;

public abstract class PaginatedListCommandHandler<T> implements UnitedCommandExecutor {

    protected static final int PAGE_SIZE = 10;

    private static final List<String> DIRECTIONS = List.of("asc", "desc");

    protected abstract Collection<T> getEntries();

    protected abstract List<ListCriterion<T>> getCriteria();

    protected abstract String getEntryName(T entry);

    protected abstract String getInfoCommand(T entry);

    protected abstract String getListCommand();

    protected abstract String getMessagePath();

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        var current = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
        var previous = new ArrayList<String>();
        for (int i = 0; i < args.length - 1; i++)
            previous.add(args[i].toLowerCase());

        var suggestions = new ArrayList<String>();
        var criterionKeys = getCriteria().stream().map(ListCriterion::key).toList();
        if (previous.stream().noneMatch(criterionKeys::contains))
            suggestions.addAll(criterionKeys);
        if (previous.stream().noneMatch(DIRECTIONS::contains))
            suggestions.addAll(DIRECTIONS);

        return suggestions.stream().filter(s -> s.startsWith(current)).collect(Collectors.toList());
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        ListCriterion<T> criterion = null;
        Boolean descending = null;
        Integer page = null;

        for (var arg : args) {
            var lower = arg.toLowerCase();
            var match = getCriteria().stream().filter(c -> c.key().equals(lower)).findFirst().orElse(null);
            if (match != null && criterion == null) {
                criterion = match;
            } else if (DIRECTIONS.contains(lower) && descending == null) {
                descending = lower.equals("desc");
            } else if (isPositiveInteger(lower) && page == null) {
                page = Integer.parseInt(lower);
            } else {
                United.messenger().send(sender, "player.lists.invalid-argument", arg,
                        getCriteria().stream().map(ListCriterion::key).collect(Collectors.joining(", ")));
                return;
            }
        }

        if (criterion == null)
            criterion = getCriteria().get(0);
        if (descending == null)
            descending = criterion.descendingByDefault();
        if (page == null)
            page = 1;

        var sorted = sort(getEntries(), criterion, descending);
        if (sorted.isEmpty()) {
            United.messenger().send(sender, "player.lists.empty");
            return;
        }

        var pageCount = (int) Math.ceil((double) sorted.size() / PAGE_SIZE);
        page = Math.min(page, pageCount);

        var direction = descending ? "desc" : "asc";
        var lines = new ArrayList<String>();
        lines.add(United.messenger().get("player.lists.sorted-by", criterion.key(),
                United.messenger().get("player.lists.direction-" + direction), sorted.size()));

        var start = (page - 1) * PAGE_SIZE;
        var end = Math.min(start + PAGE_SIZE, sorted.size());
        for (int i = start; i < end; i++) {
            var row = sorted.get(i);
            lines.add(United.messenger().get("player.lists.entry",
                    i + 1,
                    getInfoCommand(row.getKey()).replace("\\", "\\\\").replace("'", "\\'"),
                    getEntryName(row.getKey()),
                    criterion.display().apply(row.getKey(), row.getValue())));
        }

        var baseCommand = getListCommand() + " " + criterion.key() + " " + direction + " ";
        var previousLink = page > 1
                ? United.messenger().get("player.lists.previous", baseCommand + (page - 1))
                : United.messenger().get("player.lists.previous-disabled");
        var nextLink = page < pageCount
                ? United.messenger().get("player.lists.next", baseCommand + (page + 1))
                : United.messenger().get("player.lists.next-disabled");
        lines.add(United.messenger().get("player.lists.navigation", previousLink, page, pageCount, nextLink));

        United.messenger().sendFramed(sender, getMessagePath(), String.join("\n", lines));
    }

    @SuppressWarnings("rawtypes")
    private List<Map.Entry<T, Comparable>> sort(Collection<T> entries, ListCriterion<T> criterion, boolean descending) {

        Comparator<Map.Entry<T, Comparable>> comparator = Comparator.comparing(Map.Entry::getValue,
                Comparator.nullsFirst(Comparator.naturalOrder()));
        if (descending)
            comparator = comparator.reversed();
        comparator = comparator.thenComparing(e -> getEntryName(e.getKey()), String.CASE_INSENSITIVE_ORDER);

        return entries.stream()
                .map(e -> (Map.Entry<T, Comparable>) new AbstractMap.SimpleImmutableEntry<>(e, criterion.extractor().apply(e)))
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private boolean isPositiveInteger(String value) {
        if (value.isEmpty() || value.length() > 6)
            return false;
        for (var c : value.toCharArray())
            if (!Character.isDigit(c))
                return false;
        return Integer.parseInt(value) > 0;
    }

    protected static String formatDate(Object timestamp) {
        return new SimpleDateFormat("dd-MM-yyyy").format((long) timestamp);
    }

}
