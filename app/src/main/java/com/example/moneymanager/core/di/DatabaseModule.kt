package com.example.moneymanager.core.di

import android.content.Context
import androidx.room.Room
import com.example.moneymanager.core.data.MoneyDao
import com.example.moneymanager.core.data.MoneyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MoneyDatabase {
        return Room.databaseBuilder(
            context,
            MoneyDatabase::class.java,
            "money_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideMoneyDao(database: MoneyDatabase): MoneyDao {
        return database.moneyDao()
    }
}
