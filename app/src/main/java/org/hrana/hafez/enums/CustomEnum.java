package org.hrana.hafez.enums;

/**
 * Enum class for report attachment types.
 */
public class CustomEnum {

    public enum MEDIA_TYPE {
        AUDIO("audio recorder"), IMAGE("camera"), VIDEO("video camera"), FILE("import");

        private String value;

        MEDIA_TYPE(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    public enum REPORT_TAG {
        NEWS("news"), HUMAN_RIGHTS("human rights");

        private String value;

        REPORT_TAG(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }
}
