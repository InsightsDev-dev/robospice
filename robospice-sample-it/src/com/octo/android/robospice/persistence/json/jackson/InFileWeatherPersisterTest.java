package com.octo.android.robospice.persistence.json.jackson;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.app.Application;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.file.InFileObjectPersister;
import com.octo.android.robospice.sample.model.json.Curren_weather;
import com.octo.android.robospice.sample.model.json.Weather;
import com.octo.android.robospice.sample.model.json.WeatherResult;

@SmallTest
public class InFileWeatherPersisterTest extends InstrumentationTestCase {
    private static final String TEST_TEMP_UNIT = "C";
    private static final String TEST_TEMP = "28";
    private static final String TEST_TEMP2 = "30";
    private InFileObjectPersister< WeatherResult > dataPersistenceManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Application application = (Application) getInstrumentation().getTargetContext().getApplicationContext();
        JacksonObjectPersisterFactory factory = new JacksonObjectPersisterFactory( application );
        dataPersistenceManager = factory.createObjectPersister( WeatherResult.class );
    }

    @Override
    protected void tearDown() throws Exception {
        dataPersistenceManager.removeAllDataFromCache();
        super.tearDown();
    }

    public void test_canHandleClientRequestStatus() {
        boolean canHandleClientWeatherResult = dataPersistenceManager.canHandleClass( WeatherResult.class );
        assertEquals( true, canHandleClientWeatherResult );
    }

    public void test_saveDataAndReturnData() throws Exception {
        // GIVEN
        WeatherResult weatherRequestStatus = buildWeather( TEST_TEMP, TEST_TEMP_UNIT );

        // WHEN
        WeatherResult weatherReturned = dataPersistenceManager.saveDataToCacheAndReturnData( weatherRequestStatus, "weather.json" );

        // THEN
        assertEquals( TEST_TEMP, weatherReturned.getWeather().getCurren_weather().get( 0 ).getTemp() );
    }

    public void test_saveDataAndReturnData_async() throws Exception {
        // GIVEN
        WeatherResult weatherRequestStatus = buildWeather( TEST_TEMP, TEST_TEMP_UNIT );

        // WHEN
        dataPersistenceManager.setAsyncSaveEnabled( true );
        WeatherResult weatherReturned = dataPersistenceManager.saveDataToCacheAndReturnData( weatherRequestStatus, "weather.json" );

        // THEN
        ( (JacksonObjectPersister< ? >) dataPersistenceManager ).awaitForSaveAsyncTermination( 500, TimeUnit.MILLISECONDS );
        assertEquals( TEST_TEMP, weatherReturned.getWeather().getCurren_weather().get( 0 ).getTemp() );
    }

    public void test_loadDataFromCache_no_expiracy() throws Exception {
        // GIVEN
        WeatherResult weatherRequestStatus = buildWeather( TEST_TEMP, TEST_TEMP_UNIT );
        final String FILE_NAME = "toto";
        dataPersistenceManager.saveDataToCacheAndReturnData( weatherRequestStatus, FILE_NAME );

        // WHEN
        WeatherResult weatherReturned = dataPersistenceManager.loadDataFromCache( FILE_NAME, DurationInMillis.ALWAYS );

        // THEN
        assertEquals( TEST_TEMP, weatherReturned.getWeather().getCurren_weather().get( 0 ).getTemp() );
    }

    public void test_loadDataFromCache_not_expired() throws Exception {
        // GIVEN
        WeatherResult weatherRequestStatus = buildWeather( TEST_TEMP, TEST_TEMP_UNIT );
        final String FILE_NAME = "toto";
        dataPersistenceManager.saveDataToCacheAndReturnData( weatherRequestStatus, FILE_NAME );

        // WHEN
        WeatherResult weatherReturned = dataPersistenceManager.loadDataFromCache( FILE_NAME, DurationInMillis.ONE_SECOND );

        // THEN
        assertEquals( TEST_TEMP, weatherReturned.getWeather().getCurren_weather().get( 0 ).getTemp() );
    }

    public void test_loadDataFromCache_expired() throws Exception {
        // GIVEN
        WeatherResult weatherRequestStatus = buildWeather( TEST_TEMP2, TEST_TEMP_UNIT );
        final String FILE_NAME = "toto";
        dataPersistenceManager.saveDataToCacheAndReturnData( weatherRequestStatus, FILE_NAME );
        File cachedFile = ( (JacksonObjectPersister< ? >) dataPersistenceManager ).getCacheFile( FILE_NAME );
        cachedFile.setLastModified( System.currentTimeMillis() - 5 * DurationInMillis.ONE_SECOND );

        // WHEN
        WeatherResult weatherReturned = dataPersistenceManager.loadDataFromCache( FILE_NAME, DurationInMillis.ONE_SECOND );

        // THEN
        assertNull( weatherReturned );
    }

    public void test_loadAllDataFromCache_with_one_request_in_cache() throws Exception {
        // GIVEN
        WeatherResult weatherRequestStatus = buildWeather( TEST_TEMP, TEST_TEMP_UNIT );
        final String FILE_NAME = "toto";
        dataPersistenceManager.saveDataToCacheAndReturnData( weatherRequestStatus, FILE_NAME );

        // WHEN
        List< WeatherResult > listWeatherResult = dataPersistenceManager.loadAllDataFromCache();

        // THEN
        assertNotNull( listWeatherResult );
        assertEquals( 1, listWeatherResult.size() );
        assertEquals( weatherRequestStatus, listWeatherResult.get( 0 ) );
    }

    public void test_loadAllDataFromCache_with_two_requests_in_cache() throws Exception {
        // GIVEN
        WeatherResult weatherRequestStatus = buildWeather( TEST_TEMP, TEST_TEMP_UNIT );
        final String FILE_NAME = "toto";
        dataPersistenceManager.saveDataToCacheAndReturnData( weatherRequestStatus, FILE_NAME );

        WeatherResult weatherRequestStatus2 = buildWeather( TEST_TEMP2, TEST_TEMP_UNIT );
        final String FILE_NAME2 = "tutu";
        dataPersistenceManager.saveDataToCacheAndReturnData( weatherRequestStatus2, FILE_NAME2 );

        // WHEN
        List< WeatherResult > listWeatherResult = dataPersistenceManager.loadAllDataFromCache();

        // THEN
        assertNotNull( listWeatherResult );
        assertEquals( 2, listWeatherResult.size() );
        assertTrue( listWeatherResult.contains( weatherRequestStatus ) );
        assertTrue( listWeatherResult.contains( weatherRequestStatus2 ) );
    }

    public void test_loadAllDataFromCache_with_no_requests_in_cache() throws Exception {
        // GIVEN

        // WHEN
        List< WeatherResult > listWeatherResult = dataPersistenceManager.loadAllDataFromCache();

        // THEN
        assertNotNull( listWeatherResult );
        assertTrue( listWeatherResult.isEmpty() );
    }

    public void test_removeDataFromCache_when_two_requests_in_cache_and_one_removed() throws Exception {
        // GIVEN
        WeatherResult weatherRequestStatus = buildWeather( TEST_TEMP, TEST_TEMP_UNIT );
        final String FILE_NAME = "toto";
        dataPersistenceManager.saveDataToCacheAndReturnData( weatherRequestStatus, FILE_NAME );

        WeatherResult weatherRequestStatus2 = buildWeather( TEST_TEMP2, TEST_TEMP_UNIT );
        final String FILE_NAME2 = "tutu";
        dataPersistenceManager.saveDataToCacheAndReturnData( weatherRequestStatus2, FILE_NAME2 );

        dataPersistenceManager.removeDataFromCache( FILE_NAME2 );

        // WHEN
        List< WeatherResult > listWeatherResult = dataPersistenceManager.loadAllDataFromCache();

        // THEN
        assertNotNull( listWeatherResult );
        assertEquals( 1, listWeatherResult.size() );
        assertTrue( listWeatherResult.contains( weatherRequestStatus ) );
        assertFalse( listWeatherResult.contains( weatherRequestStatus2 ) );
    }

    private WeatherResult buildWeather( String temp, String tempUnit ) {
        WeatherResult weatherRequestStatus = new WeatherResult();
        Weather weather = new Weather();
        List< Curren_weather > currents = new ArrayList< Curren_weather >();
        Curren_weather current_weather = new Curren_weather();
        current_weather.setTemp( temp );
        current_weather.setTemp_unit( tempUnit );
        currents.add( current_weather );
        weather.setCurren_weather( currents );
        weatherRequestStatus.setWeather( weather );
        return weatherRequestStatus;
    }
}
