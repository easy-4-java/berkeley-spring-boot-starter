package com.sleepycat.berkeley.spring.boot;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.TransactionConfig;

public class BerkeleyTemplate {

	@Autowired
	private Database myDatabase;
	@Autowired
	private Environment myDbEnvironment;
	
	//像数据库中写数据
    public void writeToDatabase(String key, String value, boolean isOverwrite){
        try {
            //JE的记录包含两部分，key键值和value数据值，这两个值都是通过DatabaseEntry对象封装起来的
            //所以说如果要使用记录，则必须创建两个DatabaseEntry对象，一个是key，一个是value
            //DatabaseEntry内部使用的是bytes数组
            DatabaseEntry databaseKey = new DatabaseEntry(key.trim().getBytes("utf8"));
            DatabaseEntry databaseValue = new DatabaseEntry(value.trim().getBytes("utf8"));

            OperationStatus res = null;//操作状态码
            Transaction txn = null;//事务对象

            TransactionConfig txConfig = new TransactionConfig();//事务配置
            txConfig.setSerializableIsolation(true);//设置串行化隔离级别

            txn = myDbEnvironment.beginTransaction(null, txConfig);//开始事物

            if(isOverwrite)
                //添加一条记录。如数据库不支持一个key对应多个data或当前数据库中已经存在该key了，则使用此方法将使用新的值覆盖旧的值。
                res = myDatabase.put(txn, databaseKey, databaseValue);
            else
                //不管数据库是否允许支持多重记录(一个key对应多个value),只要存在该key就不允许添加，并且返回perationStatus.KEYEXIST信息
                res = myDatabase.putNoOverwrite(txn, databaseKey, databaseValue);

            txn.commit();//提交事务

            if(res == OperationStatus.SUCCESS)
                System.out.println("insert success");
            else if(res == OperationStatus.KEYEXIST)
                System.out.println("key exist");
            else
                System.out.println("insert fail");

        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //遍历数据库中数据
    public ArrayList<String> getAllFromDatabase() throws UnsupportedEncodingException{
        Cursor myCursor = null;//游标
        ArrayList<String> resultList = new ArrayList<String>();
        Transaction txn = null;

         txn = myDbEnvironment.beginTransaction(null, null);
         CursorConfig cc = new CursorConfig();//游标配置
         cc.setReadCommitted(true);//设置隔离级别

         if(myCursor==null)
             myCursor = myDatabase.openCursor(txn, cc);

         DatabaseEntry entryKey = new DatabaseEntry();
         DatabaseEntry entryValue = new DatabaseEntry(); 

         if(myCursor.getFirst(entryKey, entryValue, LockMode.DEFAULT) == OperationStatus.SUCCESS){
             String key = new String(entryKey.getData(), "UTF-8");
             resultList.add(key);
             while (myCursor.getNext(entryKey, entryValue, LockMode.DEFAULT) == OperationStatus.SUCCESS) 
             {
                 key = new String(entryKey.getData(), "UTF-8");
                 resultList.add(key);
             }
         }

         myCursor.close();

         txn.commit();
         return resultList;
    }
    //从数据库读取相应键值的数据
    public String readFromDatabase(String key){
        try {
            DatabaseEntry databaseKey = new DatabaseEntry(key.trim().getBytes("utf8"));
            DatabaseEntry databaseValue = new DatabaseEntry();
            Transaction txn = null;//事务对象

            TransactionConfig txConfig = new TransactionConfig();//事务配置
            txConfig.setSerializableIsolation(true);//设置串行化隔离级别

            txn = myDbEnvironment.beginTransaction(null, txConfig);//开始事务
            OperationStatus res = myDatabase.get(txn, databaseKey, databaseValue, LockMode.DEFAULT);

            txn.commit();//提交事务
            if(res == OperationStatus.SUCCESS){
                byte[] retData = databaseValue.getData();
                String foundData = new String(retData, "utf8");
                return foundData;
            }else{
                return "";
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "";
        }

    }

    //删除数据库中一条数据
    public void deleteFromDatabase(String key){
        Transaction txn = null;

        TransactionConfig txConfig = new TransactionConfig();
        txConfig.setSerializableIsolation(true);

        txn = myDbEnvironment.beginTransaction(null, txConfig);
        DatabaseEntry databaseKey = null;
        try {
            databaseKey = new DatabaseEntry(key.trim().getBytes("utf8"));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        OperationStatus res = myDatabase.delete(txn, databaseKey);
        txn.commit();

        if(res == OperationStatus.SUCCESS)
            System.out.println("delete success");
        else if(res == OperationStatus.KEYEMPTY)
            System.out.println("no key");
        else
            System.out.println("delete fail");
    }

    //一些其他方法
    public void otherMethod(){
        String databaseName = myDatabase.getDatabaseName();//数据库名字
        System.out.println("databaseName : " + databaseName);

        Environment env = myDatabase.getEnvironment();//取得当前数据库的环境信息
        System.out.println(env);

        List<String> list = myDbEnvironment.getDatabaseNames();//取得当前环境下数据库名称列表
        System.out.println(list);

        // JE requires the handle to be closed before truncation. Reopen it so
        // this template remains usable after clearing the records.
        com.sleepycat.je.DatabaseConfig config = myDatabase.getConfig();
        myDatabase.close();
        long deleteNum = env.truncateDatabase(null, databaseName, true);
        myDatabase = env.openDatabase(null, databaseName, config);
        System.out.println(deleteNum);



    }
    
    @SuppressWarnings("unchecked")
    public void writePrimitiveDatabase(String key, String value){
        try {
            DatabaseEntry databaseKey = new DatabaseEntry(key.trim().getBytes("utf8"));
            DatabaseEntry databaseValue = new DatabaseEntry();

            @SuppressWarnings("rawtypes")
            EntryBinding myBinding = TupleBinding.getPrimitiveBinding(String.class);
            myBinding.objectToEntry(value, databaseValue);
            myDatabase.put(null, databaseKey, databaseValue);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void readPrimitiveDatabase(String key){
        try {
            DatabaseEntry databaseKey = new DatabaseEntry(key.trim().getBytes("utf8"));
            DatabaseEntry databaseValue = new DatabaseEntry();

            @SuppressWarnings("rawtypes")
            EntryBinding myBinding = TupleBinding.getPrimitiveBinding(String.class);

            OperationStatus retVal = myDatabase.get(null, databaseKey, databaseValue,  LockMode.DEFAULT);

            if(retVal == OperationStatus.SUCCESS){
                String value = (String)myBinding.entryToObject(databaseValue);
                System.out.println(value);
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
	
	public Database getDatabase() {
		return myDatabase;
	}

	public void setDatabase(Database database) {
		this.myDatabase = database;
	}
	
}
