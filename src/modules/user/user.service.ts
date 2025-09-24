import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { GenericCrudRepository } from '../../database/postgres/repository/generic-crud.repository';
import { User } from './entities/user.entity';

@Injectable()
export class UserService {
  private readonly userRepository: GenericCrudRepository<User>;

  constructor(
    @InjectRepository(User)
    private readonly userRepo: Repository<User>,
  ) {
    this.userRepository = new GenericCrudRepository(userRepo, User.name);
  }

  async getUserByEmail(email: string): Promise<User | null> {
    return await this.userRepository.findOneOrNone({
      where: { email },
    });
  }

  async createUser(payload: Partial<User>): Promise<User> {
    const user = await this.userRepository.create(payload);
    return user;
  }
}
