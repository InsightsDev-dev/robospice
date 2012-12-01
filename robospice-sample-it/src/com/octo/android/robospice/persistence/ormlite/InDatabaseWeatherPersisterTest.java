package com.octo.android.robospice.persistence.ormlite;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.app.Application;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.ObjectPersister;
import com.octo.android.robospice.sample.model.xml.Curren_weather;
import com.octo.android.robospice.sample.model.xml.Day;
import com.octo.android.robospice.sample.model.xml.Forecast;
import com.octo.android.robospice.sample.model.xml.Night;
import com.octo.android.robospice.sample.model.xml.Weather;
import com.octo.android.robospice.sample.model.xml.Wind;

@SmallTest
public class InDatabaseWeatherPersisterTest extends InstrumentationTestCase {
    private ObjectPersister< Weather > dataPersistenceManager;
    private static final Curren_weather TEST_TEMP = new Curren_weather();
    private static final Curren_weather TEST_TEMP2 = new Curren_weather();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Application application = (Application) getInstrumentation().getTargetContext().getApplicationContext();

        List< Class< ? >> classCollection = new ArrayList< Class< ? >>();

        // add persisted classes to class collection
        classCollection.add( Weather.class );
        classCollection.add( Curren_weather.class );
        classCollection.add( Day.class );
        classCollection.add( Forecast.class );
        classCollection.add( Night.class );
        classCollection.add( Wind.class );

        RoboSpiceDatabaseHelper databaseHelper = new RoboSpiceDatabaseHelper( application, "sample_database.db", 1 );
        databaseHelper.clearTableFromDataBase( Weather.class );
        InDatabaseObjectPersisterFactory inDatabaseObjectPersisterFactory = new InDatabaseObjectPersisterFactory( application, databaseHelper, classCollection );
        dataPersistenceManager = inDatabaseObjectPersisterFactory.createObjectPersister( Weather.class );

        TEST_TEMP.setTemp( "28" );
        TEST_TEMP.setTemp_unit( "C" );
        TEST_TEMP2.setTemp( "30" );
        TEST_TEMP2.setTemp_unit( "C" );
    }

    @Override
    protected void tearDown() throws Exception {
        dataPersistenceManager.removeAllDataFromCache();
        super.tearDown();
    }

    public void test_canHandleClientRequestStatus() {
        boolean canHandleClientWeather = dataPersistenceManager.canHandleClass( Weather.class );
        assertEquals( true, canHandleClientWeather );
    }

    public void test_saveDataAndReturnData() throws Exception {
        // GIVEN
        final int CACHE_KEY = 1;
        Weather weatherRequestStatus = buildWeather( CACHE_KEY, TEST_TEMP );

        // WHEN
        Weather weatherReturned = dataPersistenceManager.saveDataToCacheAndReturnData( weatherRequestStatus, 1 );

        // THEN
        assertTrue( weatherReturned.getListWeather().contains( TEST_TEMP ) );
    }

    public void test_saveDataAndReturnData_async() throws Exception {
        // GIVEN
        final int CACHE_KEY = 1;
        Weather weatherRequestStatus = buildWeather( CACHE_KEY, TEST_TEMP );

        // WHEN
        dataPersistenceManager.setAsyncSaveEnabled( true );
        Weather weatherReturned = dataPersistenceManager.saveDataToCacheAndReturnData( weatherRequestStatus, 1 );

        // THEN
        ( (InDatabaseObjectPersister< ?, ? >) dataPersistenceManager ).awaitForSaveAsyncTermination( 500, TimeUnit.MILLISECONDS );
        assertTrue( weatherReturned.getListWeather().contains( TEST_TEMP ) );
    }

    public void test_loadDataFromCache_no_expiracy() throws Exception {
        // GIVEN
        final int CACHE_KEY = 1;
        Weather weatherRequestStatus = buildWeather( CACHE_KEY, TEST_TEMP );
        dataPersistenceManager.saveDataToCacheAndReturnData( weatherRequestStatus, CACHE_KEY );

        // WHEN
        Weather weatherReturned = dataPersistenceManager.loadDataFromCache( CACHE_KEY, DurationInMillis.ALWAYS );

        // THEN
        assertEquals( CACHE_KEY, weatherReturned.getId() );
        assertTrue( weatherReturned.getListWeather().contains( TEST_TEMP ) );
    }

    public void test_loadDataFromCache_not_expired() throws Exception {
        // GIVEN
        final int CACHE_KEY = 1;
        Weather weatherRequestStatus = buildWeather( CACHE_KEY, TEST_TEMP );
        dataPersistenceManager.saveDataToCacheAndReturnData( weatherRequestStatus, CACHE_KEY );

        // WHEN
        Weather weatherReturned = dataPersistenceManager.loadDataFromCache( CACHE_KEY, DurationInMillis.ONE_SECOND );

        // THEN
        assertTrue( weatherReturned.getListWeather().contains( TEST_TEMP ) );
    }

    public void test_loadDataFromCache_expired() throws Exception {
        // GIVEN
        final int CACHE_KEY = 1;
        Weather weatherRequestStatus = buildWeather( CACHE_KEY, TEST_TEMP );
        dataPersistenceManager.saveDataToCacheAndReturnData( weatherRequestStatus, CACHE_KEY );
        Thread.sleep( DurationInMillis.ONE_SECOND );
        // WHEN
        Weather weatherReturned = dataPersistenceManager.loadDataFromCache( CACHE_KEY, DurationInMillis.ONE_SECOND );

        // THEN
        assertNull( weatherReturned );
    }

    public void test_loadAllDataFromCache_with_one_request_in_cache() throws Exception {
        // GIVEN
        final int CACHE_KEY = 1;
        Weather weatherRequestStatus = buildWeather( CACHE_KEY, TEST_TEMP );
        dataPersistenceManager.saveDataToCacheAndReturnData( weatherRequestStatus, CACHE_KEY );

        // WHEN
        List< Weather > listWeatherResult = dataPersistenceManager.loadAllDataFromCache();

        // THEN
        assertNotNull( listWeatherResult );
        assertEquals( 1, listWeatherResult.size() );
        assertEquals( weatherRequestStatus, listWeatherResult.get( 0 ) );
    }

    public void test_loadAllDataFromCache_with_two_requests_in_cache() throws Exception {
        // GIVEN
        final int CACHE_KEY1 = 1;

        Weather weatherRequestStatus = buildWeather( CACHE_KEY1, TEST_TEMP );
        dataPersistenceManager.saveDataToCacheAndReturnData( weatherRequestStatus, CACHE_KEY1 );

        final int CACHE_KEY2 = 2;
        Weather weatherRequestStatus2 = buildWeather( CACHE_KEY2, TEST_TEMP2 );
        dataPersistenceManager.saveDataToCacheAndReturnData( weatherRequestStatus2, CACHE_KEY2 );

        // WHEN
        List< Weather > listWeatherResult = dataPersistenceManager.loadAllDataFromCache();

        // THEN
        assertNotNull( listWeatherResult );
        assertEquals( 2, listWeatherResult.size() );
        assertTrue( listWeatherResult.contains( weatherRequestStatus ) );
        assertTrue( listWeatherResult.contains( weatherRequestStatus2 ) );
    }

    public void test_loadAllDataFromCache_with_no_requests_in_cache() throws Exception {
        // GIVEN

        // WHEN
        List< Weather > listWeather = dataPersistenceManager.loadAllDataFromCache();

        // THEN
        assertNotNull( listWeather );
        assertTrue( listWeather.isEmpty() );
    }

    public void test_removeDataFromCache_when_two_requests_in_cache_and_one_removed() throws Exception {
        // GIVEN
        final int CACHE_KEY1 = 1;
        Weather weatherRequestStatus = buildWeather( CACHE_KEY1, TEST_TEMP );
        dataPersistenceManager.saveDataToCacheAndReturnData( weatherRequestStatus, CACHE_KEY1 );

        final int CACHE_KEY2 = 2;
        Weather weatherRequestStatus2 = buildWeather( CACHE_KEY2, TEST_TEMP2 );
        dataPersistenceManager.saveDataToCacheAndReturnData( weatherRequestStatus2, CACHE_KEY2 );

        dataPersistenceManager.removeDataFromCache( CACHE_KEY2 );

        // WHEN
        List< Weather > listWeatherResult = dataPersistenceManager.loadAllDataFromCache();

        // THEN
        assertNotNull( listWeatherResult );
        assertEquals( 1, listWeatherResult.size() );
        assertTrue( listWeatherResult.contains( weatherRequestStatus ) );
        assertFalse( listWeatherResult.contains( weatherRequestStatus2 ) );
    }

    private Weather buildWeather( int id, Curren_weather curren_weather ) {
        Weather weather = new Weather();
        weather.setId( id );
        List< Curren_weather > currents = new ArrayList< Curren_weather >();
        currents.add( curren_weather );
        weather.setListWeather( currents );
        weather.setListForecast( null );
        return weather;
    }
}
