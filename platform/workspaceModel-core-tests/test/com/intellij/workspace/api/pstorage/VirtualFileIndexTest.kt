// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.workspace.api.pstorage

import com.intellij.idea.Bombed
import com.intellij.workspace.api.*
import com.intellij.workspace.api.pstorage.indices.VirtualFileUrlListProperty
import com.intellij.workspace.api.pstorage.indices.VirtualFileUrlNullableProperty
import com.intellij.workspace.api.pstorage.indices.VirtualFileUrlProperty
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

internal class PVFUEntityData : PEntityData<PVFUEntity>() {
  lateinit var data: String
  lateinit var fileProperty: VirtualFileUrl
  override fun createEntity(snapshot: TypedEntityStorage): PVFUEntity {
    return PVFUEntity(data, fileProperty).also { addMetaData(it, snapshot) }
  }
}

internal class PVFUWithTwoPropertiesEntityData : PEntityData<PVFUWithTwoPropertiesEntity>() {
  lateinit var data: String
  lateinit var fileProperty: VirtualFileUrl
  lateinit var secondFileProperty: VirtualFileUrl
  override fun createEntity(snapshot: TypedEntityStorage): PVFUWithTwoPropertiesEntity {
    return PVFUWithTwoPropertiesEntity(data, fileProperty, secondFileProperty).also { addMetaData(it, snapshot) }
  }
}

internal class PNullableVFUEntityData : PEntityData<PNullableVFUEntity>() {
  lateinit var data: String
  var fileProperty: VirtualFileUrl? = null
  override fun createEntity(snapshot: TypedEntityStorage): PNullableVFUEntity {
    return PNullableVFUEntity(data, fileProperty).also { addMetaData(it, snapshot) }
  }
}

internal class PListVFUEntityData : PEntityData<PListVFUEntity>() {
  lateinit var data: String
  lateinit var fileProperty: List<VirtualFileUrl>
  override fun createEntity(snapshot: TypedEntityStorage): PListVFUEntity {
    return PListVFUEntity(data, fileProperty).also { addMetaData(it, snapshot) }
  }
}

internal class PVFUEntity(val data: String, val fileProperty: VirtualFileUrl) : PTypedEntity()
internal class PVFUWithTwoPropertiesEntity(val data: String, val fileProperty: VirtualFileUrl, val secondFileProperty: VirtualFileUrl) : PTypedEntity()
internal class PNullableVFUEntity(val data: String, val fileProperty: VirtualFileUrl?) : PTypedEntity()
internal class PListVFUEntity(val data: String, val fileProperty: List<VirtualFileUrl>) : PTypedEntity()

internal class ModifiablePVFUEntity : PModifiableTypedEntity<PVFUEntity>() {
  var data: String by EntityDataDelegation()
  var fileProperty: VirtualFileUrl by VirtualFileUrlProperty()
}

internal class ModifiablePVFUWithTwoPropertiesEntity : PModifiableTypedEntity<PVFUWithTwoPropertiesEntity>() {
  var data: String by EntityDataDelegation()
  var fileProperty: VirtualFileUrl by VirtualFileUrlProperty()
  var secondFileProperty: VirtualFileUrl by VirtualFileUrlProperty()
}

internal class ModifiablePNullableVFUEntity : PModifiableTypedEntity<PNullableVFUEntity>() {
  var data: String by EntityDataDelegation()
  var fileProperty: VirtualFileUrl? by VirtualFileUrlNullableProperty()
}

internal class ModifiablePListVFUEntity : PModifiableTypedEntity<PListVFUEntity>() {
  var data: String by EntityDataDelegation()
  var fileProperty: List<VirtualFileUrl> by VirtualFileUrlListProperty()
}

internal fun TypedEntityStorageBuilder.addPVFUEntity(data: String,
                                                     fileUrl: String,
                                                     virtualFileManager: VirtualFileUrlManager,
                                                     source: EntitySource = PSampleEntitySource("test")): PVFUEntity {
  return addEntity(ModifiablePVFUEntity::class.java, source) {
    this.data = data
    this.fileProperty = virtualFileManager.fromUrl(fileUrl)
  }
}

internal fun TypedEntityStorageBuilder.addPVFU2Entity(data: String,
                                                     fileUrl: String,
                                                     secondFileUrl: String,
                                                     virtualFileManager: VirtualFileUrlManager,
                                                     source: EntitySource = PSampleEntitySource("test")): PVFUWithTwoPropertiesEntity {
  return addEntity(ModifiablePVFUWithTwoPropertiesEntity::class.java, source) {
    this.data = data
    this.fileProperty = virtualFileManager.fromUrl(fileUrl)
    this.secondFileProperty = virtualFileManager.fromUrl(secondFileUrl)
  }
}

internal fun TypedEntityStorageBuilder.addPNullableVFUEntity(data: String,
                                                     fileUrl: String?,
                                                     virtualFileManager: VirtualFileUrlManager,
                                                     source: EntitySource = PSampleEntitySource("test")): PNullableVFUEntity {
  return addEntity(ModifiablePNullableVFUEntity::class.java, source) {
    this.data = data
    if (fileUrl != null) this.fileProperty = virtualFileManager.fromUrl(fileUrl)
  }
}

internal fun TypedEntityStorageBuilder.addPListVFUEntity(data: String,
                                                     fileUrl: List<String>,
                                                     virtualFileManager: VirtualFileUrlManager,
                                                     source: EntitySource = PSampleEntitySource("test")): PListVFUEntity {
  return addEntity(ModifiablePListVFUEntity::class.java, source) {
    this.data = data
    this.fileProperty = fileUrl.map { virtualFileManager.fromUrl(it) }
  }
}

class VirtualFileIndexTest {
  private lateinit var virtualFileManager: VirtualFileUrlManager
  @Before
  fun setUp() {
    virtualFileManager = VirtualFileUrlManagerImpl()
  }

  @Test
  fun `add entity with not null vfu`() {
    val fileUrl = "/user/opt/app/a.txt"
    val builder = PEntityStorageBuilder.create()
    val entity = builder.addPVFUEntity("hello", fileUrl, virtualFileManager)
    assertEquals(fileUrl, entity.fileProperty.url)
    assertEquals(entity.fileProperty, builder.indexes.virtualFileIndex.getVirtualFiles(entity.id)?.first())
  }

  @Test
  fun `change virtual file url`() {
    val fileUrl = "/user/opt/app/a.txt"
    val fileUrl2 = "/user/opt/app/b.txt"
    val fileUrl3 = "/user/opt/app/c.txt"
    val builder = PEntityStorageBuilder.create()
    val entity = builder.addPVFUEntity("hello", fileUrl, virtualFileManager)
    assertEquals(fileUrl, entity.fileProperty.url)

    val modifiedEntity = builder.modifyEntity(ModifiablePVFUEntity::class.java, entity) {
      this.fileProperty = virtualFileManager.fromUrl(fileUrl2)
      this.fileProperty = virtualFileManager.fromUrl(fileUrl3)
    }
    assertEquals(fileUrl3, modifiedEntity.fileProperty.url)
    val virtualFiles = builder.indexes.virtualFileIndex.getVirtualFiles(modifiedEntity.id) ?: error("")
    assertEquals(1, virtualFiles.size)
    assertEquals(modifiedEntity.fileProperty, virtualFiles.first())
  }

  @Test
  fun `add entity with nullable vfu`() {
    val builder = PEntityStorageBuilder.create()
    val entity = builder.addPNullableVFUEntity("hello", null, virtualFileManager)
    assertNull(entity.fileProperty)
    assertTrue(builder.indexes.virtualFileIndex.getVirtualFiles(entity.id)?.isEmpty() ?: false)
  }

  @Test
  @Bombed(year = 2020, month = Calendar.JUNE, day = 20, user = "Mikhail Mazurkevich")
  fun `add entity with two properties`() {
    val fileUrl = "/user/opt/app/a.txt"
    val secondUrl = "/user/opt/app/b.txt"
    val builder = PEntityStorageBuilder.create()
    val entity = builder.addPVFU2Entity("hello", fileUrl, secondUrl, virtualFileManager)
    assertEquals(fileUrl, entity.fileProperty.url)
    assertEquals(secondUrl, entity.secondFileProperty.url)

    val virtualFiles = builder.indexes.virtualFileIndex.getVirtualFiles(entity.id) ?: error("")
    assertEquals(2, virtualFiles.size)
    assertEquals(entity.fileProperty, virtualFiles.first())
    assertEquals(entity.secondFileProperty, virtualFiles.last())
  }

  @Test
  fun `add entity with vfu list`() {
    val fileUrlList = listOf("/user/a.txt", "/user/opt/app/a.txt", "/user/opt/app/b.txt")
    val builder = PEntityStorageBuilder.create()
    val entity = builder.addPListVFUEntity("hello", fileUrlList, virtualFileManager)
    assertEquals(fileUrlList, entity.fileProperty.map { it.url }.sorted())
    assertEquals(fileUrlList.size, builder.indexes.virtualFileIndex.getVirtualFiles(entity.id)?.size)
  }

  @Test
  fun `add entity to diff`() {
    val fileUrlA = "/user/opt/app/a.txt"
    val fileUrlB = "/user/opt/app/b.txt"
    val builder = PEntityStorageBuilder.create()
    val entityA = builder.addPVFUEntity("bar", fileUrlA, virtualFileManager)
    assertEquals(fileUrlA, entityA.fileProperty.url)
    assertEquals(entityA.fileProperty, builder.indexes.virtualFileIndex.getVirtualFiles(entityA.id)?.first())

    val diff = PEntityStorageBuilder.from(builder.toStorage())
    val entityB = diff.addPVFUEntity("foo", fileUrlB, virtualFileManager)
    assertEquals(fileUrlB, entityB.fileProperty.url)
    assertEquals(entityA.fileProperty, diff.indexes.virtualFileIndex.getVirtualFiles(entityA.id)?.first())
    assertEquals(entityB.fileProperty, diff.indexes.virtualFileIndex.getVirtualFiles(entityB.id)?.first())

    assertTrue(builder.indexes.virtualFileIndex.getVirtualFiles(entityB.id)?.isEmpty() ?: false)
    builder.addDiff(diff)

    assertEquals(entityA.fileProperty, builder.indexes.virtualFileIndex.getVirtualFiles(entityA.id)?.first())
    assertEquals(entityB.fileProperty, builder.indexes.virtualFileIndex.getVirtualFiles(entityB.id)?.first())
  }

  @Test
  fun `remove entity from diff`() {
    val fileUrlA = "/user/opt/app/a.txt"
    val fileUrlB = "/user/opt/app/b.txt"
    val builder = PEntityStorageBuilder.create()
    val entityA = builder.addPVFUEntity("bar", fileUrlA, virtualFileManager)
    val entityB = builder.addPVFUEntity("foo", fileUrlB, virtualFileManager)
    assertEquals(entityA.fileProperty, builder.indexes.virtualFileIndex.getVirtualFiles(entityA.id)?.first())
    assertEquals(entityB.fileProperty, builder.indexes.virtualFileIndex.getVirtualFiles(entityB.id)?.first())

    val diff = PEntityStorageBuilder.from(builder.toStorage())
    assertEquals(entityA.fileProperty, diff.indexes.virtualFileIndex.getVirtualFiles(entityA.id)?.first())
    assertEquals(entityB.fileProperty, diff.indexes.virtualFileIndex.getVirtualFiles(entityB.id)?.first())

    diff.removeEntity(entityB)
    assertEquals(entityA.fileProperty, diff.indexes.virtualFileIndex.getVirtualFiles(entityA.id)?.first())
    assertTrue(diff.indexes.virtualFileIndex.getVirtualFiles(entityB.id)?.isEmpty() ?: false)
    assertEquals(entityB.fileProperty, builder.indexes.virtualFileIndex.getVirtualFiles(entityB.id)?.first())
    builder.addDiff(diff)

    assertEquals(entityA.fileProperty, builder.indexes.virtualFileIndex.getVirtualFiles(entityA.id)?.first())
    assertTrue(builder.indexes.virtualFileIndex.getVirtualFiles(entityB.id)?.isEmpty() ?: false)
  }

  @Test
  fun `update entity in diff`() {
    val fileUrlA = "/user/opt/app/a.txt"
    val fileUrlB = "/user/opt/app/b.txt"
    val fileUrlC = "/user/opt/app/c.txt"
    val builder = PEntityStorageBuilder.create()
    val entityA = builder.addPVFUEntity("bar", fileUrlA, virtualFileManager)
    var entityB = builder.addPVFUEntity("foo", fileUrlB, virtualFileManager)
    assertEquals(entityA.fileProperty, builder.indexes.virtualFileIndex.getVirtualFiles(entityA.id)?.first())
    assertEquals(entityB.fileProperty, builder.indexes.virtualFileIndex.getVirtualFiles(entityB.id)?.first())

    val diff = PEntityStorageBuilder.from(builder.toStorage())
    assertEquals(entityA.fileProperty, diff.indexes.virtualFileIndex.getVirtualFiles(entityA.id)?.first())
    var virtualFile = diff.indexes.virtualFileIndex.getVirtualFiles(entityB.id)
    assertNotNull(virtualFile)
    assertEquals(fileUrlB, entityB.fileProperty.url)
    assertEquals(entityB.fileProperty, virtualFile!!.first())

    entityB = diff.modifyEntity(ModifiablePVFUEntity::class.java, entityB) {
      fileProperty = virtualFileManager.fromUrl(fileUrlC)
    }
    assertEquals(entityA.fileProperty, diff.indexes.virtualFileIndex.getVirtualFiles(entityA.id)?.first())
    virtualFile = diff.indexes.virtualFileIndex.getVirtualFiles(entityB.id)
    assertNotNull(virtualFile)
    assertEquals(fileUrlC, entityB.fileProperty.url)
    assertEquals(fileUrlC, virtualFile!!.first().url)
    assertNotEquals(fileUrlB, entityB.fileProperty.url)
    assertEquals(entityB.fileProperty, virtualFile.first())
    builder.addDiff(diff)

    assertEquals(entityA.fileProperty, builder.indexes.virtualFileIndex.getVirtualFiles(entityA.id)?.first())
    virtualFile = builder.indexes.virtualFileIndex.getVirtualFiles(entityB.id)
    assertNotNull(virtualFile)
    assertEquals(fileUrlC, entityB.fileProperty.url)
    assertEquals(fileUrlC, virtualFile!!.first().url)
    assertNotEquals(fileUrlB, entityB.fileProperty.url)
    assertEquals(entityB.fileProperty, virtualFile.first())
  }
}