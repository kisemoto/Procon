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

    public static int taskSum;

    /**
     * @param args
     */
    public static void main(String[] args) {
        geneLength = 0;

        try {
            // 入力JSONデータの読み込み
            ObjectMapper mapper = new ObjectMapper();
            root = mapper.readTree(new File("./data/question-0.json"));

            // タスク数
            taskSum = root.get("tasks").size();

            // 初期ゲノムサイズ設定
            for (JsonNode nTask : root.get("tasks")) {
                if (nTask.get("duration") != null) {
                    if (nTask.get("duration").asInt() > geneLength) {
                        geneLength = nTask.get("duration").asInt();
                    }
                }
            }

            System.out.println("初期遺伝子数：" + geneLength);

            boolean check = false;

            geneLength = 14;

            while (!check) {

                System.out.println("遺伝子サイズ：" + geneLength);

                // データの初期化
                Data data = new Data();
                // メンバーの初期化
                data.people = new Person[root.get("members").size()];
                for (int i = 0; i < root.get("members").size(); i++) {
                    data.people[i] = new Person();
                    data.people[i].gene = new ArrayList<Integer>();
                }
                // タスクの初期化
                data.tasks = new ArrayList<Task>();

                check = scheduling(data);
                geneLength++;

                if (geneLength > 14) {
                    check = true;
                }
            }

        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }

    private static boolean scheduling(Data data) {

        Data cloneData = data;
        int maxChildTaskEndDay = 0;

        for (JsonNode nTask : root.get("tasks")) {

            System.out.println("〇Nodetask：" + nTask.get("id").asInt());

            // 全タスク格納チェック
            if (checkTaskComplete(cloneData)) {
                System.out.println("--------------スケジュール-------------");
                for (Task taskk : cloneData.tasks) {
                    System.out.println("--------タスクID[" + taskk.id + "]--------");
                    System.out.println("タスク開始日：" + taskk.startDay);
                    System.out.println("タスク終了日：" + taskk.endDay);
                    System.out.println("作業者：" + taskk.personId);
                }
                return true;
            }

            // すでに格納しているタスクかのチェック
            if (!taskCheck(cloneData.tasks, nTask.get("id").asInt())) {
                System.out.println("すでに格納");
                continue;
            }

            // 事前作業が終わっているかのチェック
            if (nTask.get("predecessorIds") != null) {
                Object[] array = prodecessorCheck(cloneData, nTask);
                if (!(boolean) array[0]) {
                    System.out.println("事前作業終わってない");
                    continue;
                }
                maxChildTaskEndDay = (int) array[1];
            }

            // 親タスクの時
            if (nTask.get("duration") == null) {
                System.out.println("親作業");
                Object[] array = addParentTask(cloneData, nTask);
                Task task = (Task) array[1];
                if ((boolean) array[0]) {
                    cloneData.tasks.add(task);
                    // 再帰
                    if (!scheduling(cloneData)) {
                        continue;
                    }
                    else {
                        return true;
                    }
                }
                else {
                    System.out.println("親作業（子が終わってない）");
                    continue;
                }
            }

            for (JsonNode nMember : root.get("members")) {
                Task task = new Task();
                task.id = nTask.get("id").asInt();

                // すでに格納しているタスクかのチェック
                if (!taskCheck(cloneData.tasks, nTask.get("id").asInt())) {
                    System.out.println("すでに格納");
                    continue;
                }

                System.out.println("NodeMember：" + nMember.get("id").asInt());

                if (!personCheck(nTask.get("skillIds"), nMember.get("skillIds"))) {
                    System.out.println("スキルなし");
                    continue;
                }
                task.personId = nMember.get("id").asInt();
                int geneSize = cloneData.people[nMember.get("id").asInt() - 1].gene.size();
                if (nTask.get("predecessorIds") != null) {
                    if (geneSize < maxChildTaskEndDay + 1) {
                        for (int i = 0; i < maxChildTaskEndDay + 1 - geneSize; i++) {
                            cloneData.people[nMember.get("id").asInt() - 1].gene.add(1);
                        }
                    }
                }

                task.startDay = cloneData.people[nMember.get("id").asInt() - 1].gene.size();
                task.endDay = task.startDay + nTask.get("duration").asInt() - 1;
                for (int i = 0; i < task.endDay + 1; i++) {
                    cloneData.people[nMember.get("id").asInt() - 1].gene.add(1);
                }

                System.out.println("----NoWTast------");
                System.out.println("NoWTastID：" + task.id);
                System.out.println("NoWTastSTART：" + task.startDay);
                System.out.println("NoWTastEND：" + task.endDay);
                System.out.println("NoWTastPERSON：" + task.personId);
                System.out.println("---------------------");

                // 遺伝子数のサイズチェック
                if (!checkDuration(task)) {
                    System.out.println("OUT");
                    continue;
                }

                cloneData.tasks.add(task);

                System.out.println("-----------tasks-----------");
                for (Task task2 : cloneData.tasks) {
                    System.out.println("-------ID：" + task2.id + "---------");
                    System.out.println("Start：" + task2.startDay);
                    System.out.println("End：" + task2.endDay);
                    System.out.println("PersinID：" + task2.personId);
                }
                System.out.println("-----------------------");

                // 再帰
                if (!scheduling(cloneData)) {
                    continue;
                }
                else {
                    return true;
                }
            }
        }
        return false;
    }

    // ゲノムのサイズオーバー判別
    private static boolean checkDuration(Task task) {
        if (task.endDay > geneLength) {
            return false;
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
        if (taskSkillIds == null) {
            return true;
        }

        int count = 0;
        int taskSkillIdNum = taskSkillIds.size();
        for (JsonNode taskSkillId : taskSkillIds) {
            for (JsonNode memberSkillId : memberSkillIds) {
                if (taskSkillId.asInt() == memberSkillId.asInt()) {
                    count++;
                }
            }
        }
        if (count == taskSkillIdNum) {
            return true;
        }
        return false;
    }

    // 事前作業のあるタスクの関してのチェック
    private static Object[] prodecessorCheck(Data data, JsonNode nTask) {
        Object[] array = new Object[2];
        int count = 0;
        int taskProdecessorIdsNum = nTask.get("predecessorIds").size();
        // 事前作業の最大終了日
        int maxChildTaskEndDay = 0;
        array[0] = false;
        for (JsonNode predecessorId : nTask.get("predecessorIds")) {
            for (Task task : data.tasks) {
                if (predecessorId.asInt() == task.id) {
                    count++;
                    if (task.endDay > maxChildTaskEndDay) {
                        maxChildTaskEndDay = task.endDay;
                    }
                }
            }
        }
        if (count == taskProdecessorIdsNum) {
            array[0] = true;
        }
        array[1] = maxChildTaskEndDay;
        return array;
    }

    // 子タスクが終わっているとき、親タスクを完了させる
    private static Object[] addParentTask(Data data, JsonNode nTask) {
        Object[] array = new Object[2];
        array[0] = false;

        int count = 0;
        int childIdsNum = nTask.get("childIds").size();

        Task task = new Task();
        task.id = nTask.get("id").asInt();
        task.startDay = 1000000;
        task.endDay = 0;
        task.personId = 0;
        for (JsonNode childId : nTask.get("childIds")) {
            for (Task t : data.tasks) {
                if (t.id == childId.asInt()) {
                    count++;
                    if (t.startDay < task.startDay) {
                        task.startDay = t.startDay;
                    }
                    if (t.endDay > task.endDay) {
                        task.endDay = t.endDay;
                    }
                }
            }
        }
        if (count == childIdsNum) {
            array[0] = true;
        }
        array[1] = task;

        return array;
    }

    // 全タスクを格納したかのチェック
    private static boolean checkTaskComplete(Data data) {
        if (data.tasks.size() == taskSum) {
            return true;
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

    int startDay;

    int endDay;

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

