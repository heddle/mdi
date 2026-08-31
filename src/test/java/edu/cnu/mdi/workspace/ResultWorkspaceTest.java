package edu.cnu.mdi.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ResultWorkspaceTest {

    @Test
    void retainsImmutableEntriesAndEvictsOldestAtCapacity() {
        Instant now = Instant.parse("2026-08-06T12:00:00Z");
        ResultWorkspace<String> workspace = new ResultWorkspace<>(2,
                Clock.fixed(now, ZoneOffset.UTC));
        List<String> events = new ArrayList<>();
        workspace.addListener(new ResultWorkspaceListener<>() {
            @Override public void resultRetained(RetainedResult<String> entry) {
                events.add("add:" + entry.result());
            }
            @Override public void resultRemoved(RetainedResult<String> entry) {
                events.add("remove:" + entry.result());
            }
        });
        Map<String, String> metadata = new java.util.HashMap<>();
        metadata.put("units", "MeV");
        RetainedResult<String> first = workspace.retain("First", "one", metadata);
        metadata.put("units", "keV");
        workspace.retain("Second", "two");
        workspace.retain("Third", "three");

        assertEquals(2, workspace.size());
        assertFalse(workspace.find(first.id()).isPresent());
        assertEquals("MeV", first.metadata().get("units"));
        assertEquals(now, first.retainedAt());
        assertEquals(List.of("add:one", "add:two", "remove:one", "add:three"), events);
        assertThrows(UnsupportedOperationException.class,
                () -> first.metadata().put("new", "value"));
    }

    @Test
    void removalAndClearPublishExpectedEvents() {
        ResultWorkspace<Integer> workspace = new ResultWorkspace<>();
        List<Integer> removed = new ArrayList<>();
        int[] clears = { 0 };
        workspace.addListener(new ResultWorkspaceListener<>() {
            @Override public void resultRemoved(RetainedResult<Integer> entry) {
                removed.add(entry.result());
            }
            @Override public void workspaceCleared() { clears[0]++; }
        });
        var first = workspace.retain("one", 1);
        workspace.retain("two", 2);
        workspace.remove(first.id());
        workspace.clear();

        assertEquals(List.of(1, 2), removed);
        assertEquals(1, clears[0]);
        assertEquals(0, workspace.size());
        workspace.clear();
        assertEquals(1, clears[0]);
    }
}
