/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.vertx.lang.groovy;

import groovy.lang.MetaClassRegistry;
import groovy.lang.MetaMethod;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import org.codehaus.groovy.reflection.CachedClass;
import org.codehaus.groovy.runtime.m12n.ExtensionModule;
import org.codehaus.groovy.runtime.m12n.ExtensionModuleRegistry;
import org.codehaus.groovy.runtime.metaclass.MetaClassRegistryImpl;
import org.codehaus.groovy.util.FastArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VertxExtensionMethodBoostrap {

  private static boolean loaded;

  public synchronized static void install(MetaClassRegistry registry) {
    if (!loaded) {
      loaded = true;
      try {
        MetaClassRegistryImpl registryImpl = (MetaClassRegistryImpl) registry;
        ExtensionModuleRegistry moduleRegistry = registryImpl.getModuleRegistry();
        HashMap<CachedClass, List<MetaMethod>> map = new HashMap<>();
        new FastClasspathScanner().matchSubclassesOf(ExtensionModule.class, subclass -> {
          if (subclass.getSimpleName().equals("VertxExtensionModule")) {
            try {
              ExtensionModule module = subclass.newInstance();
              if (!moduleRegistry.hasModule(module.getName())) {
                moduleRegistry.addModule(module);
                for (MetaMethod metaMethod : module.getMetaMethods()) {
                  List<MetaMethod> metaMethods = map.computeIfAbsent(metaMethod.getDeclaringClass(), k -> new ArrayList<>());
                  metaMethods.add(metaMethod);
                  FastArray methods = metaMethod.isStatic() ? registryImpl.getStaticMethods() : registryImpl.getInstanceMethods();
                  methods.add(metaMethod);
                }
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }).scan();
        for (Map.Entry<CachedClass, List<MetaMethod>> e : map.entrySet()) {
          CachedClass cls = e.getKey();
          cls.setNewMopMethods(e.getValue());
        }
      } catch (Throwable ignore) {
      }
    }
  }
}