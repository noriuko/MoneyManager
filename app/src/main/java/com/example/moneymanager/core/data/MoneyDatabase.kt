package com.example.moneymanager.core.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Transaction::class, Category::class, ScheduledTransaction::class], version = 2, exportSchema = false)
abstract class MoneyDatabase : RoomDatabase() {
    abstract fun moneyDao(): MoneyDao
}
