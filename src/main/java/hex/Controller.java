package hex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class Controller {
    private final Model model;
    private final View view;
    @Autowired
    public Controller(Model model, View view){
        this.model = model;
        this.view = view;
    }

    public void run() throws IOException {
        view.printModel(model.getData());
        model.updateFile();
    }
}
