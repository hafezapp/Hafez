package org.hrana.hafez.model;

import java.util.List;

import lombok.Data;
import lombok.experimental.Builder;

/**
 * Legal contact model holding information about individual legal professionals.
 *
 * LAW_TYPE is an enum representing specialization of lawyers and is based on the classification
 * found in the (examples of "types" could be Financial Law, Corporate Law, Human Rights Law, Other).
 *
 * LegalContacts can and generally do have more than one specialization ascribed to them, so in
 * addition to attributes such as name, phone number, and office address, each LegalContact
 * entity also has a collection of LAW_TYPES.
 *
 */
@Builder
@Data
public class LegalContact {

    public enum LAW_TYPE {

        ONE(1),
        TWO(2),
        THREE(3),
        FOUR(4),
        FIVE(5),
        SIX(6);

        private int index;

        LAW_TYPE(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public static LAW_TYPE byValue(int index) {
            for (LAW_TYPE t : LAW_TYPE.values()) {
                if (t.getIndex() == index) {
                    return t;
                }
            }
            return null;
        }
    }
    private String name, description, phone, mobile, address, address2, email;
    private List<LAW_TYPE> lawType;
}
