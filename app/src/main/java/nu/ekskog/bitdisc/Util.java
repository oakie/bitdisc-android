package nu.ekskog.bitdisc;

public class Util {
    private Util() {}

    public static String getPlayerTag(String name) {
        String firstName;
        String lastName;
        try {
            firstName = name.substring(0, 3).toUpperCase();
        } catch (Exception e) {
            firstName = name.toUpperCase();
        }
        try {
            lastName = name.substring(name.lastIndexOf(" ") + 1, name.lastIndexOf(" ") + 4).toUpperCase();
        } catch (Exception e) {
            lastName = "";
        }
        return firstName + "\n" + lastName;
    }
}
