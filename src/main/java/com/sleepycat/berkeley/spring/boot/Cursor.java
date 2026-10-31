package com.sleepycat.berkeley.spring.boot;

import java.io.File;
import java.io.UnsupportedEncodingException;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

public class Cursor {   
    private EnvironmentConfig envConfig = null;//数据库环境配置对象
    private Environment myDbEnvironment = null;//数据库环境对象
    private DatabaseConfig dbConfig = null;//数据库配置对象
    private Database myDatabase = null;//数据库对象
    private com.sleepycat.je.Cursor myCursor = null; 
    private String envDir = "dbEnv";//用户指定目录，存放数据文件和日志文件
    private String dbName = "tt";//数据库名称

    //配置创建环境对象
    public void configEnvironment(){
        envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);//如果设置了true则表示当数据库环境不存在时候重新创建一个数据库环境，默认为false.
        envConfig.setTransactional(false);//事务支持,如果为true，则表示当前环境支持事务处理，默认为false，不支持事务处理。
        envConfig.setReadOnly(false);//是否以只读方式打开，默认为false.
        myDbEnvironment = new Environment(new File(envDir), envConfig);
    }

    //配置创建完环境对象后，可以用它创建数据库并打开游标
    public void createDatabase(){   
        dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);//如果设置了true则表示当数据库不存在时候重新创建一个数据库，默认为false.
        dbConfig.setTransactional(false);//事务支持,如果为true，则表示当前数据库支持事务处理，默认为false，不支持事务处理。
        dbConfig.setReadOnly(false);//是否以只读方式打开，默认为false.

        myDatabase = myDbEnvironment.openDatabase(null, dbName, dbConfig);
        myCursor = myDatabase.openCursor(null, null);  
    }

    //用cursor遍历
    public void getAllByCursor() throws UnsupportedEncodingException{
        DatabaseEntry databaseKey = new DatabaseEntry();          
        DatabaseEntry databaseValue = new DatabaseEntry();
        //向后遍历          
        while(myCursor.getPrev(databaseKey, databaseValue, LockMode.DEFAULT) == OperationStatus.SUCCESS){                 
            String theKey = new String(databaseKey.getData(), "UTF-8");               
            String theData = new String(databaseValue.getData(), "UTF-8");                
            System.out.println("Key: " + theKey + "Data: " + theData);                    
        }

        //向前遍历
        myCursor.getFirst(databaseKey, databaseValue, LockMode.DEFAULT);
        while(myCursor.getNext(databaseKey, databaseValue, LockMode.DEFAULT) == OperationStatus.SUCCESS){
            String theKey = new String(databaseKey.getData(), "UTF-8");               
            String theData = new String(databaseValue.getData(), "UTF-8");                
            System.out.println("Key: " + theKey + "Data: " + theData);                    
        }
    }

    //用游标搜索数据
    public void searchByCursor(String key, String value) throws UnsupportedEncodingException{
        //Cursor.getSearchKey()通过key的方式检索，使用后游标指针将移动到跟当前key匹配的第一项。  
        //Cursor.getSearchKeyRange()把游标移动到大于或等于查询的key的第一个匹配key,大小比较是通过你设置的比较器来完成的，如果没有设置则使用默认的比较器。  
        //Cursor.getSearchBoth()通过key和value方式检索，然后把游标指针移动到与查询匹配的第一项。  
        //Cursor.getSearchBothRange()把游标移动到所有的匹配key和大于或等于指定的data的第一项

        DatabaseEntry databaseKey = new DatabaseEntry(key.getBytes("utf8"));
        DatabaseEntry databaseValue = new DatabaseEntry(value.getBytes("utf8"));

        OperationStatus res = myCursor.getSearchBoth(databaseKey, databaseValue, LockMode.DEFAULT);
        if(res == OperationStatus.NOTFOUND){
            System.out.println("No Data Found");
        }else{
            String foundKey = new String(databaseKey.getData(),"utf8");
            String foundValue = new String(databaseValue.getData(),"utf8");
            System.out.println("key: " + foundKey + " " + "value: " + foundValue);
        }

    }

    //使用游标定义多重记录
    public void searchMultipleByCursor(String key) throws UnsupportedEncodingException{
        //如果你的库支持多重记录，你可以使用游标来遍历一个key下的多个data.  
        //Cursor.getNext(), Cursor.getPrev()获取上一条记录或下一条记录  
        //Cursor.getSearchBothRange()用语定位到满足指定data的第一条记录。  
        //Cursor.getNextNoDup(), Cursor.getPrevNoDup() 跳到上一个key的最后一个data或下一个key的第一个data,忽略 当前key多重记录的存在。  
        //Cursor.getNextDup(), Cursor.getPrevDup() 在当前key中把指针移动到前一个data或后一个data.  
        //Cursor.count()获取当前key下的data总数
        DatabaseEntry databaseKey = new DatabaseEntry(key.getBytes("utf8"));
        DatabaseEntry databaseValue = new DatabaseEntry();

        OperationStatus res  = myCursor.getSearchKey(databaseKey, databaseValue, LockMode.DEFAULT);
        if(res == OperationStatus.SUCCESS){
            while(res == OperationStatus.SUCCESS){
                String foundKey = new String(databaseKey.getData(), "utf8");
                String foundValue = new String(databaseValue.getData(), "utf8");
                System.out.println("key: " + foundKey + " " + "value: " + foundValue);
                res = myCursor.getNextDup(databaseKey, databaseValue, LockMode.DEFAULT); 
            }
        }


    }

    //通过游标添加数据
    public void addDataByCursor(String key, String value) throws UnsupportedEncodingException{
        DatabaseEntry databaseKey = new DatabaseEntry(key.getBytes("UTF-8"));             
        DatabaseEntry databaseValue = new DatabaseEntry(value.getBytes("UTF-8")); 

        OperationStatus res = myCursor.put(databaseKey, databaseValue);
        if(res == OperationStatus.SUCCESS)
            System.out.println("insert success");
        else
            System.out.println("insert fail");
    }

    //使用游标删除记录
    public void deleteDataByCursor(String key) throws UnsupportedEncodingException{
        DatabaseEntry databaseKey = new DatabaseEntry(key.getBytes("UTF-8"));         
        DatabaseEntry databaseValue = new DatabaseEntry();                        
        OperationStatus res = myCursor.getSearchKey(databaseKey, databaseValue,LockMode.DEFAULT);             
        //如果date不是多重记录.               
        if (myCursor.count() == 1) {                          
            myCursor.delete();//删除当前记录
        }
    }

    //修改游标当前位置所在的值
    public void changeCursor(String key) throws UnsupportedEncodingException{
        //可以通过Cursor.putCurrent()方法来修改，这个方法只有一个参数就是将要修改的值。这个方法不能用在多重记录
        DatabaseEntry databaseKey = new DatabaseEntry(key.getBytes("UTF-8"));
        DatabaseEntry databaseValue = new DatabaseEntry();
        myCursor.getSearchKey(databaseKey, databaseValue,LockMode.DEFAULT); 
        String replaceStr = "www.baike.com";
        DatabaseEntry databaseReplace = new DatabaseEntry(replaceStr.getBytes("UTF-8"));  
        myCursor.putCurrent(databaseReplace);//把当前位置用新值替换  


    }
    //关闭数据库及游标
    public void closeDatabase(){
        if(myCursor != null) {                
            myCursor.close();     
        }  

        if(myDatabase != null)
            myDatabase.close();

        if(myDbEnvironment != null){
            myDbEnvironment.cleanLog();
            myDbEnvironment.close();
        }
    }


    public static void main(String[] args) throws UnsupportedEncodingException{
        Cursor cursor = new Cursor();
        if (args.length > 0) cursor.envDir = args[0];
        cursor.configEnvironment();
        cursor.createDatabase();
        cursor.addDataByCursor("a", "www.baidu.com");
        cursor.searchByCursor("a", "www.baidu.com");
        cursor.changeCursor("a");
        cursor.searchByCursor("a", "www.baike.com");
        cursor.deleteDataByCursor("a");
        cursor.closeDatabase();
    }

}
