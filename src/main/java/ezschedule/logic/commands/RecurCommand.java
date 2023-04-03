package ezschedule.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.List;

import ezschedule.commons.core.Messages;
import ezschedule.commons.core.index.Index;
import ezschedule.logic.commands.exceptions.CommandException;
import ezschedule.logic.parser.CliSyntax;
import ezschedule.model.Model;
import ezschedule.model.event.Date;
import ezschedule.model.event.Event;
import ezschedule.model.event.RecurFactor;

/**
 * Recurs an existing event in the scheduler.
 */
public class RecurCommand extends Command {

    public static final String COMMAND_WORD = "recur";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Recurs event in the scheduler by "
            + "the index number used in the displayed event list. "
            + "\nParameters: INDEX (must exist)\n "
            + CliSyntax.PREFIX_DATE + "ending date\n "
            + CliSyntax.PREFIX_EVERY + "{day, week, month} "
            + "\nExample: \n" + COMMAND_WORD + " 1 "
            + CliSyntax.PREFIX_DATE + "2024-02-20 "
            + CliSyntax.PREFIX_EVERY + "month ";

    public static final String MESSAGE_SUCCESS = "Recurring event added: %1$s";
    public static final String MESSAGE_RECUR_FACTOR_CAP = "Recur factor is not appropriate for "
            + "number of recurring events.";

    private final Index index;
    private final Date endDate;
    private final RecurFactor factor;

    /**
     * Creates an AddEventCommand to add the specified {@code Event}
     */
    public RecurCommand(Index index, Date endDate, RecurFactor factor) {
        requireNonNull(index);
        requireNonNull(endDate);
        requireNonNull(factor);
        this.index = index;
        this.endDate = endDate;
        this.factor = factor;
    }
    @Override
    public String commandWord() {
        return COMMAND_WORD;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Event> lastShownList = model.getFilteredEventList();

        if (index.getZeroBased() >= lastShownList.size()) {
            throw new CommandException(String.format(Messages.MESSAGE_INVALID_EVENT_DISPLAYED_INDEX,
                    index.getZeroBased() + 1));
        }

        Event eventToRecur = lastShownList.get(index.getZeroBased());
        model.clearRecent();

        switch (factor.toString()) {

        case "day":
            addEventPerDay(model, eventToRecur);
            break;

        case "week":
            addEventPerWeek(model, eventToRecur);
            break;

        case "month":
            addEventPerMonth(model, eventToRecur);
            break;

        default:
            break;
        }
        model.recentCommand().add(this);
        model.updateFilteredEventList(Model.PREDICATE_SHOW_ALL_EVENTS);

        return new CommandResult(String.format(MESSAGE_SUCCESS, eventToRecur));
    }

    /**
     * Adds new events to every day until endDate.
     * @param model model to add
     * @param eventToRecur event to recur in the model
     */
    public void addEventPerDay(Model model, Event eventToRecur) throws CommandException {

        int maxDaysInMonth = 30;

        Date baseDate = eventToRecur.getDate();
        int daysDiff = (int) baseDate.getDaysBetween(endDate.date);

        if (daysDiff > maxDaysInMonth) {
            throw new CommandException(String.format(MESSAGE_RECUR_FACTOR_CAP));
        }

        Date newDate = new Date(eventToRecur.getDate().date.plusDays(1).toString());
        Event nextEventToRecur =
                new Event(eventToRecur.getName(), newDate,
                        eventToRecur.getStartTime(), eventToRecur.getEndTime());

        for (int i = 0; i < daysDiff; i++) {
            model.addEvent(nextEventToRecur);
            model.addRecentEvent(nextEventToRecur);
            newDate = new Date(nextEventToRecur.getDate().date.plusDays(1).toString());
            nextEventToRecur =
                    new Event(eventToRecur.getName(), newDate,
                            eventToRecur.getStartTime(), eventToRecur.getEndTime());
        }
    }

    /**
     * Adds new events to every week until endDate.
     * @param model model to add
     * @param eventToRecur event to recur in the model
     */
    public void addEventPerWeek(Model model, Event eventToRecur) throws CommandException {

        int daysPerWeek = 7;
        int maxWeeksInYear = 52;

        Date baseDate = eventToRecur.getDate();
        int weeksDiff = (int) baseDate.getDaysBetween(endDate.date) / daysPerWeek;
        Date newDate = new Date(eventToRecur.getDate().date.plusWeeks(1).toString());
        Event nextEventToRecur =
                new Event(eventToRecur.getName(), newDate,
                        eventToRecur.getStartTime(), eventToRecur.getEndTime());

        if (weeksDiff > maxWeeksInYear) {
            throw new CommandException(String.format(MESSAGE_RECUR_FACTOR_CAP));
        }

        for (int i = 0; i < weeksDiff; i++) {
            model.addEvent(nextEventToRecur);
            model.addRecentEvent(nextEventToRecur);
            newDate = new Date(nextEventToRecur.getDate().date.plusWeeks(1).toString());
            nextEventToRecur =
                    new Event(eventToRecur.getName(), newDate,
                            eventToRecur.getStartTime(), eventToRecur.getEndTime());
        }
    }

    /**
     * Adds new events to every month until endDate.
     * @param model model to add
     * @param eventToRecur event to recur in the model
     */
    public void addEventPerMonth(Model model, Event eventToRecur) throws CommandException {

        int maxMonthInYear = 12;

        Date baseDate = eventToRecur.getDate();
        int monthsDiff = (int) baseDate.getMonthsBetween(endDate.date);
        Date newDate = new Date(eventToRecur.getDate().date.plusMonths(1).toString());
        Event nextEventToRecur =
                new Event(eventToRecur.getName(), newDate,
                        eventToRecur.getStartTime(), eventToRecur.getEndTime());

        if (monthsDiff > maxMonthInYear) {
            throw new CommandException(String.format(MESSAGE_RECUR_FACTOR_CAP));
        }

        for (int i = 0; i < monthsDiff; i++) {
            model.addEvent(nextEventToRecur);
            model.addRecentEvent(nextEventToRecur);
            newDate = new Date(nextEventToRecur.getDate().date.plusMonths(1).toString());
            nextEventToRecur =
                    new Event(eventToRecur.getName(), newDate,
                            eventToRecur.getStartTime(), eventToRecur.getEndTime());
        }
    }

    @Override
    public boolean equals(Object other) {
        // short circuit if same object
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof RecurCommand)) {
            return false;
        }

        // state check
        RecurCommand e = (RecurCommand) other;
        return index.equals(e.index)
                && endDate.equals(e.endDate)
                && factor.equals(e.factor);
    }
}
