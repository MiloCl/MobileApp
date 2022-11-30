package be.kuleuven.sleepshift;

public class Land {
    private int tijdzone;
    private int zomeruur;
    private String naam;

    public Land(String naam){
        this.naam=naam;
    }

    public int getTijdzone() {
        return tijdzone;
    }

    public String getNaam() {
        return naam;
    }

    public void setTijdzone(int tijdzone) {
        this.tijdzone = tijdzone;
    }

    public int getZomeruur() {
        return zomeruur;
    }

    public void setZomeruur(int zomeruur) {
        this.zomeruur = zomeruur;
    }
}
