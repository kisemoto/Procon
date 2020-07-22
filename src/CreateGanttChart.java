import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author kisemoto1
 * 
 */

public class CreateGanttChart {

    public static int geneLength;

    public static ArrayList<Data> datasList = new ArrayList<Data>();

    public static JsonNode root;

    /**
     * @param args
     */
    public static void main(String[] args) {
        geneLength = 0;
        Data data = new Data();

        try {
            ObjectMapper mapper = new ObjectMapper();
            root = mapper.readTree(new File("./data/question-0.json"));

            // 初期ゲノムサイズ設定
            for (JsonNode nTask : root.get("tasks")) {
                if (nTask.get("duration") != null) {
                    if (nTask.get("duration").asInt() > geneLength) {
                        geneLength = nTask.get("duration").asInt();
                    }
                }
            }

            // メンバー数の設定
            data.people = new Person[root.get("members").size()];
            for (int i = 0; i < root.get("members").size(); i++) {
                data.people[i] = new Person();
                data.people[i].gene = new ArrayList<Integer>();
            }

            // タスク数の設定
            data.tasks = new ArrayList<Task>();

            System.out.println("初期遺伝子数：" + geneLength);
            System.out.println("メンバー数：" + data.people.length);

            boolean check = false;

            while (!check) {
                check = scheduling(data);
                geneLength++;
                System.out.println("遺伝子数：" + geneLength);
            }

        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }

    private static boolean scheduling(Data data) {

        Data cloneData = data;

        if (!checkDuration(cloneData)) {
            return false;
        }

        for (JsonNode nTask : root.get("tasks")) {
            if (nTask.get("duration") == null || !taskCheck(cloneData.tasks, nTask.get("id").asInt())) {
                return false;
            }

            Task task = new Task();
            task.id = nTask.get("id").asInt();
            task.duration = nTask.get("duration").asInt();
            for (JsonNode nMember : root.get("members")) {
                if (!personCheck(nTask.get("skillIds"), nMember.get("skillIds"))) {
                    return false;
                }
                task.personId = nMember.get("id").asInt();
                task.start = cloneData.people[nMember.get("id").asInt() - 1].gene.size();
                for (int i = 0; i < task.duration; i++) {
                    cloneData.people[nMember.get("id").asInt() - 1].gene.add(1);
                }

                cloneData.tasks.add(task);

                if (!scheduling(cloneData)) {
                    return false;
                }
            }

        }
        for (Task task2 : cloneData.tasks) {
            System.out.println(task2.id);
            System.out.println(task2.duration);
            System.out.println(task2.start);
            System.out.println(task2.personId);
        }
        return true;
    }

    // ゲノムのサイズオーバー判別
    private static boolean checkDuration(Data data) {
        for (Person person : data.people) {
            if (person.gene != null) {
                if (person.gene.size() > geneLength) {
                    datasList.add(data);
                    return false;
                }
            }
        }
        return true;
    }

    // すでに存在するタスクか判別
    private static boolean taskCheck(ArrayList<Task> tasks, int taskId) {
        for (Task task : tasks) {
            if (taskId == task.id) {
                return false;
            }
        }
        return true;
    }

    // タスクを実行するスキルを持ってるか
    private static boolean personCheck(JsonNode taskSkillIds, JsonNode memberSkillIds) {
        for (JsonNode i : taskSkillIds) {
            for (JsonNode j : memberSkillIds) {
                if (i.asInt() == j.asInt()) {
                    return true;
                }
            }
        }
        return false;
    }

}

class Data {

    ArrayList<Task> tasks;

    Person[] people;
}

class Task {

    int id;

    int duration;

    int start;

    int personId;
}

class Person {

    ArrayList<Integer> gene;
}

/**
 * 一応入力Jsonのクラスを作った 今のところJsonNode使用のため不要
 */
// class InputSkill{
// int id;
// String name;
//
// public InputSkill(int id, String name) {
// this.id = id;
// this.name = name;
// }
// }
//
// class InputTask{
// int id;
// String name;
// int duration;
// String[] skillIds;
// String[] predecessorIds;
//
// public InputTask(int id, String name, int duration, String[] skillIds , String[] predecessorIds) {
// this.id = id;
// this.name = name;
// this.duration = duration;
// this.skillIds = skillIds;
// this.predecessorIds = predecessorIds;
// }
// }
//
// class InputMember{
// int id;
// String name;
// String[] skillIds;
//
// public InputMember(int id, String name, String[] skillId) {
// this.id = id;
// this.name = name;
// this.skillIds = skillId;
//
// }
// }

