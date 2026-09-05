package in.dreadedlama.dndsync.shared;

import java.io.Serializable;

public class WearSignal implements Serializable {

    // 0 = UNKNOWN
    // 1 = ALL
    // 2 = PRIORITY
    // 3 = NONE
    // 4 = ALARMS
    public Integer dndState = null;

    // 0 = Bedtime OFF
    // 1 = Bedtime ON
    // 2 = NO CHANGE
    public Integer bedtimeState = 2;

    public WearSignal(Integer dndState, Integer bedtimeState) {

        if (dndState != null && dndState >= 0 && dndState <= 4) {
            this.dndState = dndState;
        }

        if (bedtimeState != null && bedtimeState >= 0 && bedtimeState <= 2) {
            this.bedtimeState = bedtimeState;
        }
    }
}