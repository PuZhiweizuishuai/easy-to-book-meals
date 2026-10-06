// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'dish_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Dish _$DishFromJson(Map<String, dynamic> json) => Dish(
      id: json['id'] as int?,
      name: json['name'] as String?,
      createUser: json['createUser'] as int?,
      storefrontId: json['storefrontId'] as int?,
      description: json['description'] as String?,
      topImage: json['topImage'] as String?,
      imageList: json['imageList'] as String?,
      priceType: json['priceType'] as int?,
      basePrice: (json['basePrice'] as num?)?.toDouble(),
      categoryIds: (json['categoryIds'] as List<dynamic>?)
          ?.map((e) => CategorySummary.fromJson(e as Map<String, dynamic>))
          .toList(),
      monthlySales: json['monthlySales'] as int?,
      allSales: json['allSales'] as int?,
      stock: json['stock'] as int?,
      status: json['status'] as int?,
      createTime: json['createTime'] as int?,
      updateTime: json['updateTime'] as int?,
    );

Map<String, dynamic> _$DishToJson(Dish instance) => <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'createUser': instance.createUser,
      'storefrontId': instance.storefrontId,
      'description': instance.description,
      'topImage': instance.topImage,
      'imageList': instance.imageList,
      'priceType': instance.priceType,
      'basePrice': instance.basePrice,
      'categoryIds': instance.categoryIds?.map((e) => e.toJson()).toList(),
      'monthlySales': instance.monthlySales,
      'allSales': instance.allSales,
      'stock': instance.stock,
      'status': instance.status,
      'createTime': instance.createTime,
      'updateTime': instance.updateTime,
    };
