package org.ecmtracker.client.di

import android.content.Context
import android.net.ConnectivityManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.ecmtracker.client.data.remote.RemoteDataSource
import org.ecmtracker.client.data.remote.RemoteService
import org.ecmtracker.client.data.repository.RemoteRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideRetrofit(gson: Gson) : Retrofit = Retrofit.Builder()
//        .baseUrl(BuildConfig.API_KEY)
        .baseUrl("http://54.169.20.116/mte/api/v3/startTrip/EcMtcr!/")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    fun provideService(retrofit: Retrofit): RemoteService = retrofit.create(RemoteService::class.java)

    @Singleton
    @Provides
    fun provideRemoteDataSource(remoteService: RemoteService) = RemoteDataSource(remoteService)


    @Singleton
    @Provides
    fun provideRepository(remoteDataSource: RemoteDataSource) =
        RemoteRepository(remoteDataSource)


    @Singleton
    @Provides
    fun provideConnectivityManager( @ApplicationContext context: Context) : ConnectivityManager {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }


}