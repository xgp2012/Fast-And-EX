plugins {
    id("java")
}

//应用特定插件
apply(plugin = "chocolate_gradle")

//导入插件的任务
buildscript{
    repositories{
        //maven {url = uri("E:\\Project\\chocolate_gradle\\repo")}
        maven { url = uri("https://www.jitpack.io") }
    }
    dependencies {
        classpath("com.github.howxu:chocolate_gradle:v1.4")
    }
}


//UTF8中文支持
tasks.withType<JavaCompile>{
    options.encoding="UTF-8"
}

group = "cn.howxu.exclient"
version = "2.2"

repositories {
    mavenCentral()
    //Mojang官方库，MC相关依赖(netty/icu4j/codecjorbis/oshi等)都在这，且稳定
    maven { url = uri("https://libraries.minecraft.net/")}
    //tv.twitch等少数库在这找；该源不稳定(偶发522)，放最后作兜底
    maven { url = uri("https://nexus.velocitypowered.com/repository/maven-public/")}
}

//println(extensions.getByName("chocolate"))

//TODO Why I can't use extension?