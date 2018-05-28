# EasySQL
使用起来很简单的sql工具，支持链式操作

使用方法
--

 - **gradle**

第一步：将其添加到存储库末尾的根build.gradle中：
```
allprojects {
	repositories {
		maven { url 'https://jitpack.io' }
	}
}
```
第二步：添加到依赖

```
dependencies {
    implementation 'com.github.qixuefeng:EasySQL:0.0.1.2.1'
}
```


数据库操作
---

 - **创建数据库**

```
EasySQL.with(context).createDB(dbName); // 不用加.db
```

 - **删除数据库**

```
EasySQL.with(context).deleteDatabase(dbName);
```

 - **使用指定数据库**

```
EasySQL.with(context).use(dbName);
```

 - **获取数据库列表**

```
EasySQL.with(context).listName();
```

 - **获取某数据库中所有表**

```
EasySQL.with(context).use(dbName).tableList()
```

表操作
---

 - **创建表**

表实体需继承EasyTable，并且需要有一个**无参构造方法**，目前支持的数据类型有以下8种：

```
byte
long
float
short
byte[]
double
String
boolean
int
```
具体使用：
```
EasySQL.with(context).use(dbName).createTable(表实体.class) // 默认带自增长ID
EasySQL.with(context).use(dbName).createTable(表实体.class, boolean hasID) // 是否携带ID
EasySQL.with(context).use(dbName).createTable(表实体.class, 表实体.class, 表实体.class, ... ) // 是否携带ID
```

 - **删除表**

```
EasySQL.with(context).use(dbName).deleteTable(TypeEntity.class); 删除某数据库中某表
```

增删改查
----

 - **增**

新增的数据需要保存在EasyEntity实体类中，支持响应式添加，支持单个实体添加或者集合添加
```
EasyEntity entity = new EasyEntity();
Table1 table1 = new Table1(); // 各种set
table1EasyEntity.add(table1);
EasySQL.with(context).use(dbName).save(entity);
```

 - **删**

```
// 后面的两个参数跟Android自带的参数用法基本一样
EasySQL.with(context).use(dbName).delete(Table1.class, "_short = ?", "2");

```

 - **改**

```
//更新某表的某条记录
EasySQL.with(this).use(dbName).update(typeEntity1, "_short = ?", "2");
```

 - **查**

```
// 查询表（Table1）中的所有数据
ArrayList<Table1> retrieve1 = EasySQL.with(context).use(trim).retrieve(Table1.class);
```

最后
--
在使用上遇到什么问题或者有什么好的建议 欢迎联系我，或者发布Issues。