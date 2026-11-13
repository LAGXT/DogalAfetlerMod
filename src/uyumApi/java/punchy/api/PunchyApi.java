package punchy.api;

import java.util.function.BooleanSupplier;
import org.joml.Matrix4f;

/**
 * Derleme zamanı taslağı (jar'a girmez): Punchy 3.2a'nın {@code punchy.api.PunchyApi} sınıfından yalnızca sıcaklık/durum
 * köprümüzün kullandığı genel imzalar ({@code javap -p} ile doğrulanmıştır). Gerçek sınıf çalışma zamanında Punchy jar'ından
 * gelir; Punchy yüklü değilse {@code client.uyum.PunchyIstemciOlaylari} bu sınıfı hiç yüklemez.
 */
public final class PunchyApi {
    private PunchyApi() {
    }

    public static boolean isActive() {
        return false;
    }

    public static State state() {
        return null;
    }

    public static boolean clipExists(String ad) {
        return false;
    }

    public static long playClip(String sahip, ClipRequest istek) {
        return 0L;
    }

    public static void moment(String ad) {
    }

    public static void condition(String sahip, String ad, BooleanSupplier kosul) {
    }

    public static void armStates(String sahip, ArmStateLook gorunum) {
    }

    public static void armPose(String sahip, String kimlik, ArmPoseModifier degistirici) {
    }

    public interface Clip {
        String name();

        String layer();

        float time();

        float length();

        boolean looping();

        boolean holding();

        int arms();

        long playback();

        long ticket();

        String reason();

        boolean token(String jeton);

        float tokenTime(String jeton);
    }

    public interface State {
        long tick();

        boolean active();

        boolean firstPerson();

        boolean leftHanded();

        Clip clip(String katman);

        Clip clipOn(boolean anaEl);

        String equipPhase(boolean anaEl);

        String kind(boolean anaEl);

        boolean is(String durum);
    }

    public static final class ClipRequest {
        private ClipRequest(String klip) {
        }

        public static ClipRequest named(String klip) {
            return new ClipRequest(klip);
        }

        public ClipRequest hand(boolean anaEl) {
            return this;
        }

        public ClipRequest bothArms() {
            return this;
        }

        public ClipRequest speed(float hiz) {
            return this;
        }

        public ClipRequest blendIn(float sure) {
            return this;
        }

        public ClipRequest blendOut(float sure) {
            return this;
        }

        public ClipRequest kick(boolean tekme) {
            return this;
        }

        public ClipRequest protect(boolean koru) {
            return this;
        }
    }

    public interface ArmStates {
        ArmStates burn(float yogunluk);

        ArmStates frost(float yogunluk);

        ArmStates mud(int kademe);
    }

    public interface ArmStateLook {
        void states(Object oyuncu, boolean solKol, ArmStates durumlar);
    }

    public interface ArmPose {
        boolean leftArm();

        boolean mainHand();

        float partialTick();

        Object player();

        Object stack();

        ArmPose moveView(float x, float y, float z);

        ArmPose turnView(float x, float y, float z);

        ArmPose moveLocal(float x, float y, float z);

        ArmPose turnLocal(float x, float y, float z);

        ArmPose scale(float olcek);

        ArmPose pivot(String kemik);

        Matrix4f rig();
    }

    public interface ArmPoseModifier {
        void modify(ArmPose poz);
    }
}
