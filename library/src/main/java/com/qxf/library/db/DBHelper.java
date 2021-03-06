package com.qxf.library.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import com.qxf.library.constant.EasySQLConstants;
import com.qxf.library.utils.SQLUtils;
import com.qxf.library.utils.SharedPreferencesUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据库操作类
 */
public class DBHelper extends SQLiteOpenHelper {

	private static final String TAG = "DBHelper";

	/**
	 * 数据库名字
	 */
	private String name;

	/**
	 * 数据库操作类
	 */
	private SQLiteDatabase db;

	private Context context;

	public DBHelper(Context context, String name) {
		super(context, name, null, 1);
		this.context = context;
		this.name = name;
		db = getWritableDatabase();
	}

	/**
	 * 获取数据库名字
	 *
	 * @return 数据库名字
	 */
	public String getName() {
		return name;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		this.db = db;
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	/**
	 * 更新表
	 *
	 * @param sql 新增列
	 */
	public void updateTable(String sql) {
		db.execSQL(sql);
	}

	/**
	 * 在当前数据库中创建表
	 *
	 * @param classzz 数据库表，属性名字代表表中的的字段
	 * @return 数据库操作类
	 */
	public DBHelper createTable(Class<? extends EasyTable> classzz) {
		return createTable(classzz, classzz.getSimpleName().toLowerCase());
	}

	/**
	 * 连续创建表
	 *
	 * @param classes 数据库表，属性名字代表表中的的字段
	 * @return 数据库操作类
	 */
	@SafeVarargs
	public final DBHelper createTable(Class<? extends EasyTable>... classes) {
		for (Class<? extends EasyTable> aClass : classes) {
			createTable(aClass);
		}
		return this;
	}

	/**
	 * 创建表
	 *
	 * @param classzz   类表
	 * @param tableName 表名
	 * @return 数据库操作类
	 */
	private DBHelper createTable(Class<? extends EasyTable> classzz, String tableName) {
		return createTable(classzz, tableName, true);
	}

	/**
	 * 创建表
	 *
	 * @param classzz 类表
	 * @param hasID   是否携带自增长ID
	 * @return 数据库操作类
	 */
	public DBHelper createTable(Class<? extends EasyTable> classzz, boolean hasID) {
		return createTable(classzz, classzz.getSimpleName(), hasID);
	}

	/**
	 * 创建表
	 *
	 * @param classzz   类表
	 * @param tableName 表名
	 * @param hasID     是否携带自增长ID
	 * @return 数据库操作类
	 */
	private DBHelper createTable(Class<? extends EasyTable> classzz, String tableName, boolean hasID) {
		if (TextUtils.equals("table", tableName.toLowerCase())) {
			throw new SQLiteException("表名不能为table");
		}
		// 保存类表的类信息
		saveTable(classzz);

		String tableSQL = SQLUtils.getTableSQL(classzz, tableName, hasID);
		db.execSQL(tableSQL);
		return this;
	}

	/**
	 * 保存类表的类信息
	 *
	 * @param classzz 类表
	 */
	private void saveTable(Class<? extends EasyTable> classzz) {

		// 得到数据库下所有的表
		Set<String> dbTable = SharedPreferencesUtils.getStringSet(EasySQLConstants.EASYSQL_SHARED, name, new HashSet<String>());

		dbTable.add(classzz.getName());

		SharedPreferencesUtils.putStringSet(EasySQLConstants.EASYSQL_SHARED, name, dbTable);

	}

	/**
	 * 删除数据库
	 *
	 * @param dbName 数据库名字
	 * @return 是否成功删除
	 */
	public boolean deleteDatabase(String dbName) {
		return context.deleteDatabase(dbName);
	}

	/**
	 * 获取类表
	 *
	 * @return 类表 列表
	 */
	public ArrayList<String> tableClassList() {

		Set<String> stringSet = SharedPreferencesUtils.getStringSet(EasySQLConstants.EASYSQL_SHARED, name, new HashSet<String>());

		ArrayList<String> classTableList = new ArrayList<>();

		classTableList.addAll(stringSet);

		return classTableList;
	}

	/**
	 * 获取表中所有字段
	 *
	 * @param table 指定表
	 * @return 以集合形式返回所有字段
	 */
	public ArrayList<String> tableFieldsList(Class<? extends EasyTable> table) {

		String simpleName = table.getSimpleName();

		Cursor c = db.rawQuery("SELECT * FROM " + simpleName, null);

		String[] columnNames = c.getColumnNames();

		return new ArrayList<>(Arrays.asList(columnNames));

	}

	/**
	 * 获取指定数据库中所有的表
	 *
	 * @return 表集合
	 */
	public ArrayList<String> tableList() {

		Cursor cursor = db.query("sqlite_master", null, "type = ?", new String[]{"table"}, null, null, null);

		ArrayList<String> data = new ArrayList<>();

		if (cursor.moveToFirst()) {

			do {

				String _string = cursor.getString(cursor.getColumnIndex("name"));
				if (!TextUtils.equals(_string, "sqlite_sequence")) {
					if (!TextUtils.equals(_string, "android_metadata")) {
						data.add(_string);
					}
				}

			} while (cursor.moveToNext());

		}

		return data;

	}

	/**
	 * 保存数据
	 *
	 * @param entity
	 */
	public DBHelper save(EasyEntity entity) {

		for (int i = 0; i < entity.getDatas().size(); i++) {

			try {

				// 插入数据的时候，如果表不存在，就先创建再添加数据
				boolean hasTable = SQLUtils.hasTable(entity.getDatas().get(i).getClass(), this);
				if (!hasTable) {
					createTable(entity.getDatas().get(i).getClass());
				}

				ContentValues contentValues = SQLUtils.getContentValues(entity, i);
				db.insert(entity.getDatas().get(i).getClass().getSimpleName(), null, contentValues);
			} catch (IllegalAccessException e) {
				Log.e(TAG, "save: " + entity.getDatas().get(i).getClass().getSimpleName() + "表不存在");
				e.printStackTrace();
			} catch (SQLException e) {
				Log.e(TAG, "save: " + entity.getDatas().get(i).getClass().getSimpleName() + "表不存在");
			}

		}

		return this;

	}

	/**
	 * 保存数据
	 *
	 * @param easyTable
	 */
	public DBHelper save(EasyTable easyTable) {

		ContentValues contentValues;
		try {

			// 插入数据的时候，如果表不存在，就先创建再添加数据
			boolean hasTable = SQLUtils.hasTable(easyTable.getClass(), this);
			if (!hasTable) {
				createTable(easyTable.getClass());
			}

			contentValues = SQLUtils.getContentValues(easyTable);
			db.insert(easyTable.getClass().getSimpleName(), null, contentValues);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		return this;

	}

	/**
	 * 保存数据
	 *
	 * @param tables 多条数据
	 */
	public DBHelper save(EasyTable... tables) {
		for (EasyTable table : tables) {
			save(table);
		}
		return this;
	}

	/**
	 * 更新数据
	 *
	 * @param t           数据实体
	 * @param whereClause sql语句
	 * @param whereArgs   限制条件
	 * @param <T>         the EasyTable
	 */
	public <T extends EasyTable> DBHelper update(T t, String whereClause, String... whereArgs) {

		String tableName = t.getClass().getSimpleName();

		try {
			db.update(tableName, SQLUtils.getContentValues(t), whereClause, whereArgs);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return this;
	}

	/**
	 * 删除表
	 *
	 * @param classzz 类表
	 */
	public DBHelper deleteTable(Class<? extends EasyTable> classzz) {
		try {
			db.execSQL(EasySQLConstants.SQL_DROP + EasySQLConstants.SQL_SPACE + classzz.getSimpleName());
		} catch (SQLException e) {
			Log.e(TAG, "deleteTable: 该表不存在");
		}
		return this;
	}

	/**
	 * 清空表中数据
	 *
	 * @param classzz 类表
	 */
	public DBHelper clearTable(Class<? extends EasyTable> classzz) {
		try {
			db.execSQL(EasySQLConstants.SQL_DELETE + EasySQLConstants.SQL_SPACE + classzz.getSimpleName());
		} catch (SQLException e) {
			Log.e(TAG, "clearTable: " + classzz.getSimpleName() + "表不存在");
			e.printStackTrace();
		}
		return this;
	}

	/**
	 * 得到指定表中数据
	 *
	 * @param classzz 指定表
	 * @param <T>     数据库表
	 * @return 集合数据
	 */
	public <T extends EasyTable> ArrayList<T> retrieve(Class<T> classzz) {
		return retrieve(classzz, null, false);
	}

	/**
	 * 得到指定表中数据 排序（升序）
	 *
	 * @param classzz 指定表
	 * @param field   使用哪个字段升序
	 * @param <T>     数据库表
	 * @return 升序后的数据的集合数据
	 */
	public <T extends EasyTable> ArrayList<T> retrieve(Class<T> classzz, String field) {
		return retrieve(classzz, field, true);
	}

	/**
	 * 得到指定表中数据 排序（升序或者降序）
	 *
	 * @param classzz 指定表
	 * @param field   使用哪个字段排序
	 * @param isAsc   是否升序，否则降序
	 * @param <T>     数据库表
	 * @return 升序或者降序后的集合数据
	 */
	public <T extends EasyTable> ArrayList<T> retrieve(Class<T> classzz, String field, boolean isAsc) {
		return retrieve(classzz, null, field, isAsc);
	}

	/**
	 * 查询指定表中的指定字段
	 *
	 * @param classzz 指定表
	 * @param columns 需要被查询的字段
	 * @param <T>     数据库表
	 * @return 数据集合
	 */
	public <T extends EasyTable> ArrayList<T> retrieve(Class<T> classzz, String[] columns) {
		return retrieve(classzz, columns, null, null);
	}

	/**
	 * 得到表中指定的字段 并排序（升序或者降序）
	 *
	 * @param classzz 指定表
	 * @param columns 需要被查询的字段
	 * @param field   使用哪个字段排序
	 * @param isAsc   是否升序，否则降序
	 * @param <T>     数据库表
	 * @return
	 */
	public <T extends EasyTable> ArrayList<T> retrieve(Class<T> classzz, String[] columns, String field, boolean isAsc) {
		return retrieve(classzz, columns, null, null, field, isAsc);
	}

	/**
	 * 得到表中指定的字段 升序
	 *
	 * @param classzz 指定表
	 * @param columns 需要被查询的字段
	 * @param field   使用哪个字段排序
	 * @param <T>     数据库表
	 * @return
	 */
	public <T extends EasyTable> ArrayList<T> retrieve(Class<T> classzz, String[] columns, String field) {
		return retrieve(classzz, columns, null, null, field, true);
	}

	/**
	 * 经过条件筛选后的数据集合 升序
	 *
	 * @param classzz       表
	 * @param columns       需要被查询的字段
	 * @param selection     条件
	 * @param selectionArgs 条件的值
	 * @param <T>           数据库表
	 * @return 经过条件筛选后的数据集合 升序
	 */
	public <T extends EasyTable> ArrayList<T> retrieve(Class<T> classzz, String[] columns, String selection, String[] selectionArgs) {
		return retrieve(classzz, columns, selection, selectionArgs, null, true);
	}

	/**
	 * 经过条件筛选后的数据集合 并排序
	 *
	 * @param classzz       表
	 * @param columns       需要被查询的字段
	 * @param selection     条件
	 * @param selectionArgs 条件的值
	 * @param field         使用哪个字段排序
	 * @param <T>           数据库表
	 * @return 经过条件筛选后的数据集合 并排序
	 */
	public <T extends EasyTable> ArrayList<T> retrieve(Class<T> classzz, String[] columns, String selection, String[] selectionArgs, String field) {
		return retrieve(classzz, columns, selection, selectionArgs, field, true);
	}

	/**
	 * @param classzz       表
	 * @param columns       待查询的字段
	 * @param selection     条件
	 * @param selectionArgs 条件的值
	 * @param field         根据哪个字段排序
	 * @param isAsc         是否为升序
	 * @param <T>           数据库表
	 * @return 返回排序后的并经过条件筛选后的数据集合
	 */
	public <T extends EasyTable> ArrayList<T> retrieve(Class<T> classzz, String[] columns, String selection, String[] selectionArgs, String field, boolean isAsc) {


		ArrayList<T> query = new ArrayList<>();

		Cursor cursor = null;

		try {
			String orderBy = field + EasySQLConstants.SQL_SPACE + EasySQLConstants.SQL_ASC;
			if (!isAsc) orderBy = field + EasySQLConstants.SQL_SPACE + EasySQLConstants.SQL_DESC;
			if (TextUtils.isEmpty(field)) orderBy = null;
			cursor = db.query(classzz.getSimpleName().toLowerCase(), columns, selection, selectionArgs, null, null, orderBy);
		} catch (SQLiteException e) {
			Log.e(TAG, "retrieve: 该表或者该字段不存在");
			return query;
		}

		if (cursor != null) {

			try {
				query = SQLUtils.query(classzz, cursor);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			}
		}

		return query;

	}

	/**
	 * 删除指定表中的数据
	 *
	 * @param classzz     指定表
	 * @param whereClause sql语句
	 * @param whereArgs   限制
	 * @param <T>         the EasyTable
	 */
	public <T extends EasyTable> DBHelper delete(Class<T> classzz, String whereClause, String... whereArgs) {
		try {
			db.delete(classzz.getSimpleName(), whereClause, whereArgs);
		} catch (SQLException e) {
			Log.e(TAG, "delete: " + classzz.getSimpleName() + "表不存在");
			e.printStackTrace();
		}
		return this;
	}

	/**
	 * 清除指定表中数据
	 *
	 * @param classzz 指定表
	 * @param <T>     the EasyTable
	 */
	public <T extends EasyTable> DBHelper clear(Class<T> classzz) {
		delete(classzz, "", "");
		return this;
	}

}
