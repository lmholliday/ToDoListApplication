package utils;

import com.google.gson.*;
import domain.TimeStamp;
import domain.ToDoItem;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import exceptions.ParameterIsNotJsonStringException;
import org.javatuples.Pair;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import javax.swing.*;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CloudUtils {

    private HttpRequestFactory requestFactory;
    public String todosURL = "https://todoserver222.herokuapp.com/todos/";
    public String teamURL = "https://todoserver222.herokuapp.com/teamone/todos/";

    public CloudUtils() {
        requestFactory = new NetHttpTransport().createRequestFactory();
    }

    public boolean checkConnection(){
        try {
            HttpRequest getRequest = requestFactory.buildGetRequest(
                    new GenericUrl(todosURL));
            String rawResponse = getRequest.execute().parseAsString();
            return !rawResponse.isEmpty();
        } catch(IOException e){
            return false;
        }
    }

    public String uploadItemToCloud(ToDoItem toDoItem){
        Map<String, Object> data = new LinkedHashMap<>();
        try {
            data.put("about", toDoItem.about);
            data.put("owner", "teamone");
            data.put("due_date", toDoItem.dueDate);
            data.put("created_date", toDoItem.createdDate);
            data.put("status", toDoItem.status);
            data.put("category", toDoItem.itemCategory);
            if (!toDoItem.id.equals("-1")){
                data.put("id", toDoItem.id);
            }
            HttpContent content = new UrlEncodedContent(data);
            HttpRequest postRequest = requestFactory.buildPostRequest(
                    new GenericUrl(todosURL), content);
            postRequest.execute();
            return "Success";
        } catch (NullPointerException | IOException e){
            return "Failure";
        }
    }

    public String uploadListToCloud(List<ToDoItem> toDoItemList) throws IOException{
        Map<String, Object> data = new LinkedHashMap<>();
        try {
            for (ToDoItem tdi : toDoItemList) {
                data.put("about", tdi.about);
                data.put("owner", tdi.owner);
                data.put("due_date", tdi.dueDate);
                data.put("created_date", tdi.createdDate);
                data.put("status", tdi.status);
                data.put("category", tdi.itemCategory);
                HttpContent content = new UrlEncodedContent(data);
                HttpRequest postRequest = requestFactory.buildPostRequest(
                        new GenericUrl(todosURL), content);
                postRequest.execute();
            }
            return "Success";
        } catch (NullPointerException e){
            return "Empty List";
        }
    }

    public List<ToDoItem> readCloud(){
        try {
            return parseCloudJSONString(retrieveCloud());
        } catch (ParameterIsNotJsonStringException e){
            e.printStackTrace();
            return null;
        }
    }

    public String retrieveCloud(){
        try{
            HttpRequest getRequest = requestFactory.buildGetRequest(new GenericUrl(teamURL));
            return getRequest.execute().parseAsString();
        } catch (IOException e){
            e.printStackTrace();
            return "";
        }
    }

    public PieDataset getPieData(){
        String rawData;
        List<ToDoItem> toDoItems;
        List<Pair<String, Integer>> pairs;
        try {
            //Jacob you can do a single line to create the pairs, =UIUtils.convertListOfToDosToListOfPairs(readCloud());
            rawData = retrieveCloud();
            toDoItems = parseCloudJSONString(rawData);
            pairs = UIUtils.convertListOfToDosToListOfPairs(toDoItems);
        } catch (Exception | ParameterIsNotJsonStringException e) {
            JOptionPane.showMessageDialog(null, "Couldn't get data!");
            return new DefaultPieDataset();
        }
        return UIUtils.convertPairsToPieDataset(pairs);
    }

    //Returns number of to do items in format readable by UI popup message.
    public String calculateTotalCategoriesAndTotalItems(){
        List<ToDoItem> toDoItems = readCloud();
        String result;
        int total = 0;
        int numCompleted = 0;
        int numInProgress = 0;
        int numSnoozed = 0;

        for (ToDoItem item : toDoItems){
            String status = item.getStatus().toLowerCase();
            if (status.equals("completed")){
                numCompleted+=1;
            }else if(status.equals("in-progress")){
                numInProgress+=1;
            }else if(status.equals("snoozed")){
                numSnoozed+=1;
            }else{
                total-=1; //this is to counteract the adding of the total since it isn't applicable to the chart.
            }
            total+=1;
        }
        //populating result array with correct numbers.

        result = "Total: " + total + "\nCompleted: " + numCompleted + "\nIn Progress: " + numInProgress + "\nSnoozed: " + numSnoozed;
        return result;
    }


    public List<ToDoItem> parseCloudJSONString(String jsonString) throws ParameterIsNotJsonStringException {
        if (thisIsNotAJSONString(jsonString)) {
            throw new ParameterIsNotJsonStringException();
        }
        List<ToDoItem> list = new LinkedList<>();
        JsonParser jsonParser = new JsonParser();
        JsonElement rootElement = jsonParser.parse(jsonString);
        JsonArray rootObjects = rootElement.getAsJsonArray();
        if(rootObjects.size() > 0) {
            for (JsonElement rootObject : rootObjects) {
                var about = getStringFieldFromObject(rootObject,"about");
                var owner = getStringFieldFromObject(rootObject,"owner");
                var dueDateJson = getStringFieldFromObject(rootObject,"due_date");
                var createdDateJson = getStringFieldFromObject(rootObject,"created_date");
                var status = getStringFieldFromObject(rootObject,"status");
                var category = getStringFieldFromObject(rootObject,"category");
                var idNumber = getStringFieldFromObject(rootObject,"id");
                list.add(new ToDoItem(about, owner, new TimeStamp(dueDateJson), new TimeStamp(createdDateJson), status, category, idNumber));
            }
        } else {
            return null;
        }
        return list;
    }

    public String getStringFieldFromObject(JsonElement rootObject,String fieldName){
        try{
            return rootObject.getAsJsonObject().getAsJsonPrimitive(fieldName).getAsString();
        } catch (NullPointerException e){
            return "n/a";
        }
    }

    private boolean thisIsNotAJSONString(String json){
        return json.charAt(0) == '{' && json.charAt(0) == '[';
    }

    public String deleteSingleItem(String identifier){
        JsonParser jsonParser = new JsonParser();
        JsonElement rootElement = jsonParser.parse(retrieveCloud());
        JsonArray rootObjects = rootElement.getAsJsonArray();
        for (JsonElement rootObject : rootObjects){
            String idString = rootObject.getAsJsonObject().getAsJsonPrimitive("id").getAsString();
            var id = rootObject.getAsJsonObject().getAsJsonPrimitive("id").getAsString();
            if(idString.equals(identifier)){
                deleteTodoItem(id);
                return "Cloud Delete: Success";
            }
        }
        return "Not in cloud";
    }

    public void deleteTodoItem(String id) {
        try {
            HttpRequest deleteRequest = requestFactory.buildDeleteRequest(
                    new GenericUrl(todosURL + id));
            deleteRequest.execute();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    //Extremely destructive to anyone else using the cloud. Completely wipes every to-do item in our team name. Only use in extreme cases.
    public void clearTheCloud(){
        JsonParser jsonParser = new JsonParser();
        JsonElement rootElement = jsonParser.parse(retrieveCloud());
        JsonArray rootObjects = rootElement.getAsJsonArray();
        for (JsonElement rootObject : rootObjects){
            var number = rootObject.getAsJsonObject().getAsJsonPrimitive("id").getAsString();
            deleteTodoItem(number);
        }
    }

    public void deleteCloudEntriesSpecific(int start, int end){
        for (int i = start; i < end; i++){
            deleteTodoItem(Integer.toString(i));
        }
    }

    public void deleteCloudEntriesSpecific(String id){
            deleteTodoItem(id);
    }

}
