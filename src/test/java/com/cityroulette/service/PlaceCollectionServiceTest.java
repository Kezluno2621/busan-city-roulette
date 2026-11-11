package com.cityroulette.service;

import com.cityroulette.dto.importing.NormalizedPlace;
import com.cityroulette.dto.importing.NormalizedProviderPage;
import com.cityroulette.dto.importing.PlaceImportResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlaceCollectionServiceTest {
    @Test
    void inspectsReportedTotalWithoutWriting() {
        var provider = new RecordingProvider(237);
        var service = service(provider);

        var result = service.inspect("TEST", 20);

        assertThat(result.reportedTotalCount()).isEqualTo(237);
        assertThat(result.sampleCount()).isEqualTo(20);
        assertThat(provider.requests).containsExactly("1:20");
    }

    @Test
    void paginatesToExplicitLimitAndUsesShortFinalPage() {
        var provider = new RecordingProvider(237);
        var service = service(provider);

        var result = service.collect("TEST", true, 20, 50);

        assertThat(result.fetched()).isEqualTo(50);
        assertThat(provider.requests).containsExactly("1:20", "2:20", "3:20");
    }

    @Test
    void rejectsPageSizeAboveCollectorMaximum() {
        assertThatThrownBy(() -> service(new RecordingProvider(237)).collect("TEST", true, 101, 101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private PlaceCollectionService service(PlaceDataProvider provider) {
        PlaceImportService importer = mock(PlaceImportService.class);
        when(importer.importItems(eq("TEST"), anyList(), eq(true))).thenAnswer(invocation -> {
            List<?> items = invocation.getArgument(1);
            return new PlaceImportResult("TEST", true, items.size(), items.size(), 0, 0, 0, 0);
        });
        return new PlaceCollectionService(List.of(provider), importer, mock(ImportRunService.class));
    }

    private static final class RecordingProvider implements PlaceDataProvider {
        private final int total;
        private final List<String> requests = new ArrayList<>();

        private RecordingProvider(int total) { this.total = total; }
        public String provider() { return "TEST"; }
        public NormalizedProviderPage fetch(int pageNo, int pageSize) {
            requests.add(pageNo + ":" + pageSize);
            int offset = (pageNo - 1) * pageSize;
            int count = Math.max(0, Math.min(pageSize, total - offset));
            return new NormalizedProviderPage(Collections.nCopies(count, (NormalizedPlace) null), pageNo, total, 0);
        }
    }
}
