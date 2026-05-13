package engine.components;

public class DialogueChoiceComponent {
    public String title = "";
    public String[] choiceLabels = new String[0];
    public String[] choiceValues = new String[0];
    public boolean visible;
    public int interactingEntityId = -1;
    public int selectedChoiceIndex = -1;
    public String selectedChoiceValue = "";
}
