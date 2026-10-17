
package storage;

import model.Item;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class FileItemRepository {

    private final String fileName;

    public FileItemRepository(String fileName) {
        this.fileName = fileName;
    }

    // Save all items to a file
    public void saveItems(ArrayList<Item> items) throws IOException {

        try (ObjectOutputStream output =
                new ObjectOutputStream(
                    new FileOutputStream(fileName))) {

            output.writeObject(items);
        }
    }

    // Load all items from a file
    @SuppressWarnings("unchecked")
    public ArrayList<Item> loadItems()
            throws IOException, ClassNotFoundException {

        File file = new File(fileName);

        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }

        try (ObjectInputStream input =
                new ObjectInputStream(
                    new FileInputStream(fileName))) {

            return (ArrayList<Item>) input.readObject();
        }
    }
}
