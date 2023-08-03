import hex.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;


public class Test1 {
    public static void main(String[] args) throws IOException, InterruptedException {
        ApplicationContext applicationContext =
                new ClassPathXmlApplicationContext("ApplicationContext.xml");
        Controller controller = applicationContext.getBean("controller", Controller.class);
        controller.run();
    }
}
