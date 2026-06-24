package com.afet.monitoring.domain.service.alert;

import com.afet.monitoring.domain.event.EarthquakeDetectedEvent;
import com.afet.monitoring.domain.exception.AlertDispatchException;
import com.afet.monitoring.domain.model.RiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Command Pattern — verifies each command's execute/undo, and the dispatcher's two
 * value-adds: retry on transient failure and all-or-nothing rollback of a batch.
 */
@DisplayName("Alert commands execute/undo; the dispatcher retries and rolls back")
class AlertCommandTest {

    private static EarthquakeDetectedEvent event(double magnitude, RiskLevel level) {
        return new EarthquakeDetectedEvent(
                1L, magnitude, 10.0, 40.7654, 29.1234, "STA-YLV",
                Instant.parse("2026-06-21T00:00:00Z"), 80.0, level);
    }

    /** Records sends and retracts; can be told to fail its next {@code failTimes} sends. */
    private static final class FakeChannel implements AlertChannel {
        final List<String> sent = new ArrayList<>();
        final List<String> retracted = new ArrayList<>();
        int failTimes = 0;
        private int seq = 0;

        @Override
        public String send(String message) {
            if (failTimes > 0) {
                failTimes--;
                throw new RuntimeException("transient channel error");
            }
            sent.add(message);
            return "h" + (++seq);
        }

        @Override
        public void retract(String handle) {
            retracted.add(handle);
        }
    }

    @Test
    @DisplayName("notify-responders sends a formatted page and undo retracts it")
    void notify_execute_and_undo() {
        FakeChannel channel = new FakeChannel();
        NotifyRespondersCommand cmd = new NotifyRespondersCommand(channel, event(6.4, RiskLevel.HIGH));

        cmd.execute();
        assertThat(channel.sent).hasSize(1);
        assertThat(channel.sent.get(0)).contains("RESPONDERS:", "M6.4", "HIGH");

        cmd.undo();
        assertThat(channel.retracted).containsExactly("h1");
    }

    @Test
    @DisplayName("magnitude is formatted with a dot even on a comma locale")
    void message_uses_dot_decimal_under_turkish_locale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            FakeChannel channel = new FakeChannel();
            new BroadcastPublicWarningCommand(channel, event(7.8, RiskLevel.CRITICAL)).execute();

            assertThat(channel.sent.get(0)).contains("M7.8").doesNotContain("M7,8");
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    @DisplayName("the below-threshold command records but its undo is a no-op")
    void log_command_undo_is_noop() {
        FakeChannel channel = new FakeChannel();
        LogBelowThresholdCommand cmd = new LogBelowThresholdCommand(channel, event(3.2, RiskLevel.LOW));

        cmd.execute();
        cmd.undo();

        assertThat(channel.sent).hasSize(1);
        assertThat(channel.retracted).isEmpty(); // nothing to retract
    }

    @Test
    @DisplayName("dispatcher retries a transient failure and eventually succeeds")
    void dispatcher_retries_until_success() {
        FakeChannel channel = new FakeChannel();
        channel.failTimes = 2; // fail twice, succeed on the 3rd attempt
        AlertDispatcher dispatcher = new AlertDispatcher(3);

        dispatcher.dispatch(new NotifyRespondersCommand(channel, event(6.0, RiskLevel.HIGH)));

        assertThat(channel.sent).hasSize(1); // succeeded exactly once, after retries
    }

    @Test
    @DisplayName("dispatcher gives up after maxAttempts and throws")
    void dispatcher_gives_up() {
        FakeChannel channel = new FakeChannel();
        channel.failTimes = 99; // always fails
        AlertDispatcher dispatcher = new AlertDispatcher(3);

        assertThatThrownBy(() ->
                dispatcher.dispatch(new NotifyRespondersCommand(channel, event(6.0, RiskLevel.HIGH))))
                .isInstanceOf(AlertDispatchException.class)
                .hasMessageContaining("after 3 attempt(s)");
        assertThat(channel.sent).isEmpty();
    }

    @Test
    @DisplayName("a failing batch is rolled back — earlier commands are undone")
    void batch_rolls_back_on_failure() {
        FakeChannel channel = new FakeChannel();
        AlertDispatcher dispatcher = new AlertDispatcher(2);

        NotifyRespondersCommand ok = new NotifyRespondersCommand(channel, event(8.1, RiskLevel.CRITICAL));
        AlertCommand alwaysFails = new AlertCommand() {
            @Override public void execute() { throw new RuntimeException("boom"); }
            @Override public void undo() { /* never executed */ }
            @Override public String description() { return "always-fails"; }
        };

        assertThatThrownBy(() -> dispatcher.dispatchBatch(List.of(ok, alwaysFails)))
                .isInstanceOf(AlertDispatchException.class)
                .hasMessageContaining("always-fails");

        // the first command had been sent, then rolled back by undo -> retract
        assertThat(channel.sent).hasSize(1);
        assertThat(channel.retracted).containsExactly("h1");
    }

    @Test
    @DisplayName("a fully successful batch leaves nothing retracted and returns the executed commands")
    void successful_batch() {
        FakeChannel channel = new FakeChannel();
        AlertDispatcher dispatcher = new AlertDispatcher(2);

        List<AlertCommand> executed = dispatcher.dispatchBatch(List.of(
                new NotifyRespondersCommand(channel, event(8.1, RiskLevel.CRITICAL)),
                new BroadcastPublicWarningCommand(channel, event(8.1, RiskLevel.CRITICAL))));

        assertThat(channel.sent).hasSize(2);
        assertThat(channel.retracted).isEmpty();
        assertThat(executed).hasSize(2);
    }
}
