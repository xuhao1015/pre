package com.xd.pre.register;

import cn.hutool.core.io.file.FileReader;
import cn.hutool.db.Db;

public class IndertDataMysqlTo {
    public static Db db = Db.use();

    public static void main(String[] args) throws Exception {
        FileReader fileReader = new FileReader("C:\\Users\\Administrator\\Desktop\\git\\test.txt");
        String result = fileReader.readString();
        String[] split = result.split("\\n");
        for (String s : split) {
            try {
                int execute = db.execute(s);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        System.out.println(split);
    }
}
